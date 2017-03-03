package org.graylog.aws.processors.instancelookup;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterface;
import com.amazonaws.services.ec2.model.InstancePrivateIpAddress;
import com.amazonaws.services.ec2.model.NetworkInterface;
import com.amazonaws.services.ec2.model.NetworkInterfacePrivateIpAddress;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InstanceLookupTable {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceLookupTable.class);

    private static InstanceLookupTable INSTANCE = new InstanceLookupTable();

    enum InstanceType {
        RDS,
        EC2,
        ELB,
        UNKNOWN
    }

    private boolean loaded = false;

    private ImmutableMap<String, Instance> ec2Instances;
    private ImmutableMap<String, NetworkInterface> networkInterfaces;

    // TODO METRICS

    public static InstanceLookupTable getInstance() {
        return INSTANCE;
    }

    private InstanceLookupTable() { /* nope */ }

    public void reload(List<Regions> regions) {
        LOG.info("Reloading AWS instance lookup table.");

        ImmutableMap.Builder<String, Instance> ec2InstancesBuilder = ImmutableMap.<String, Instance>builder();
        ImmutableMap.Builder<String, NetworkInterface> networkInterfacesBuilder = ImmutableMap.<String, NetworkInterface>builder();

        for (Regions region : regions) {
            try {
                AmazonEC2Client ec2Client = new AmazonEC2Client();
                ec2Client.configureRegion(region);

                // Load network interfaces
                LOG.debug("Requesting AWS network interface descriptions in [{}].", region.getName());
                DescribeNetworkInterfacesResult interfaces = ec2Client.describeNetworkInterfaces();
                for (NetworkInterface iface : interfaces.getNetworkInterfaces()) {
                    LOG.debug("Discovered network interface [{}].", iface.getNetworkInterfaceId());

                    // Add all private IP addresses.
                    for (final NetworkInterfacePrivateIpAddress privateIp : iface.getPrivateIpAddresses()) {
                        LOG.debug("Network interface [{}] has private IP: {}", iface.getNetworkInterfaceId(), privateIp);
                        networkInterfacesBuilder.put(privateIp.getPrivateIpAddress(), iface);
                    }

                    // Add public IP address.
                    if (iface.getAssociation() != null) {
                        String publicIp = iface.getAssociation().getPublicIp();
                        LOG.debug("Network interface [{}] has public IP: {}", iface.getNetworkInterfaceId(), publicIp);
                        networkInterfacesBuilder.put(publicIp, iface);
                    }
                }

                // Load EC2 instances
                LOG.debug("Requesting EC2 instance descriptions in [{}].", region.getName());
                DescribeInstancesResult ec2Result = ec2Client.describeInstances();
                for (Reservation reservation : ec2Result.getReservations()) {
                    LOG.debug("Fetching instances for reservation [{}].", reservation.getReservationId());
                    for (final Instance instance : reservation.getInstances()) {
                        LOG.debug("Discovered EC2 instance [{}].", instance.getInstanceId());

                        // Add all private IP addresses.
                        for (InstanceNetworkInterface iface : instance.getNetworkInterfaces()) {
                            for (InstancePrivateIpAddress privateIp : iface.getPrivateIpAddresses()) {
                                LOG.debug("EC2 instance [{}] has private IP: {}", instance.getInstanceId(), privateIp.getPrivateIpAddress());
                                ec2InstancesBuilder.put(privateIp.getPrivateIpAddress(), instance);
                            }
                        }

                        // Add public IP address.
                        String publicIp = instance.getPublicIpAddress();
                        if (publicIp != null) {
                            LOG.debug("EC2 instance [{}] has public IP: {}", instance.getInstanceId(), publicIp);
                            ec2InstancesBuilder.put(publicIp, instance);
                        }
                    }
                }
            } catch(Exception e) {
                LOG.error("Error when trying to refresh AWS instance lookup table in [{}]", region.getName(), e);
            }
        }

        ec2Instances = ec2InstancesBuilder.build();
        networkInterfaces = networkInterfacesBuilder.build();

        this.loaded = true;
    }

    public DiscoveredInstance findByIp(String ip) {
        try {
            // Let's see if this is an EC2 instance maybe?
            if (ec2Instances.containsKey(ip)) {
                Instance instance = ec2Instances.get(ip);
                LOG.debug("Found IP [{}] in EC2 instance lookup table.", ip);
                return new DiscoveredEC2Instance(instance.getInstanceId(), getNameOfInstance(instance));
            }

            // If it's not an EC2 instance, we might still find it in our list of network interfaces.
            if(networkInterfaces.containsKey(ip)) {
                NetworkInterface iface = networkInterfaces.get(ip);
                switch (determineType(iface)) {
                    case RDS:
                        return new DiscoveredRDSInstance(null, null);
                    case EC2:
                        // handled directly via separate EC2 table above
                        break;
                    case ELB:
                        return new DiscoveredELBInstance(getELBNameFromInterface(iface), null);
                    case UNKNOWN:
                        LOG.debug("IP [{}] in table of network interfaces but of unknown instance type.", ip);
                        return DiscoveredInstance.UNDISCOVERED;
                }
            }

            // The IP address is not known to us. This most likely means it is an external public IP.
            return DiscoveredInstance.UNDISCOVERED;
        } catch(Exception e) {
            LOG.error("Error when trying to match IP to AWS instance. Marking as undiscovered.", e);
            return DiscoveredInstance.UNDISCOVERED;
        }
    }

    // Format is "ELB [name]" where "name" can only be a-z, A-Z and hyphens.
    private String getELBNameFromInterface(NetworkInterface iface) {
        try {
            String[] parts = iface.getDescription().split(" ");
            if (parts.length == 2) {
                return parts[1];
            } else {
                LOG.warn("Unexpected ELB name in network interface description: [{}]", iface.getDescription());
                return "unknown-name";
            }
        } catch(Exception e) {
            LOG.warn("Could not get ELB name from network interface description. Description was [{}]", iface.getDescription(), e);
            return "unknown-name";
        }
    }

    /*
     * BUT I WOULD SHAVE 500 YAKS AND I WOULD SHAVE 500 MORE
     * JUST TO BE THE GIRL WHO SHAVES 1,000 YAKS TO MERGE YOUR PR
     *
     * The AWS network interface API is not very helpful and we have to get creative to
     * figure out what kind of service an interface is attached to.
     *
     * ༼ノಠل͟ಠ༽ノ ︵ ┻━┻
     */
    private InstanceType determineType(NetworkInterface iface) {
        String ownerId;
        if(iface.getAssociation() != null) {
            ownerId = iface.getAssociation().getIpOwnerId();
        } else if(iface.getRequesterId().equals("amazon-rds")) {
            ownerId = "amazon-rds";
        } else {
            LOG.debug("AWS network interface with no association: [{}]", iface.getDescription());
            return InstanceType.UNKNOWN;
        }

        // not using switch here because it might become nasty complicated for other instance types
        if("amazon".equals(ownerId)) {
            return InstanceType.EC2;
        } else if("amazon-elb".equals(ownerId)) {
            return InstanceType.ELB;
        } else if("amazon-rds".equals(ownerId)) {
            return InstanceType.RDS;
        } else {
            return InstanceType.UNKNOWN;
        }
    }

    private String getNameOfInstance(Instance x) {
        for (Tag tag : x.getTags()) {
            if("Name".equals(tag.getKey())) {
                return tag.getValue();
            }
        }

        return null;
    }

    public boolean isLoaded() {
        return loaded;
    }

}
