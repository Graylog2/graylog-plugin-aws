package org.graylog.aws.processors.instancelookup;

import com.amazonaws.auth.AWSCredentials;
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
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ImmutableMap<String, NetworkInterface> ipAddresses;
    private ImmutableMap<String, Instance> ec2Instances;
    //private ImmutableMap<String, LoadBalancerDescription> elbInstances;

    // TODO support RDS
    // TODO METRICS

    public static InstanceLookupTable getInstance() {
        return INSTANCE;
    }

    private InstanceLookupTable() { /* nope */ }

    public void reload(AWSCredentials credentials) {
        try {
            LOG.info("Reloading AWS instance lookup table.");

            ImmutableMap.Builder<String, NetworkInterface> ipAddressesBuilder = ImmutableMap.<String, NetworkInterface>builder();
            ImmutableMap.Builder<String, Instance> ec2InstancesBuilder = ImmutableMap.<String, Instance>builder();

            AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);
            // TODO support all regions
            ec2Client.configureRegion(Regions.EU_WEST_1);

            // Load network interfaces
            LOG.debug("Requesting AWS network interface descriptions.");
            DescribeNetworkInterfacesResult interfaces = ec2Client.describeNetworkInterfaces();
            for (NetworkInterface iface : interfaces.getNetworkInterfaces()) {
                LOG.debug("Discovered network interface [{}].", iface.getNetworkInterfaceId());

                // Add all private IP addresses.
                for (final NetworkInterfacePrivateIpAddress privateIp : iface.getPrivateIpAddresses()) {
                    LOG.debug("Network interface [{}] has private IP: {}", iface.getNetworkInterfaceId(), privateIp);
                    ipAddressesBuilder.put(privateIp.getPrivateIpAddress(), iface);
                }

                // Add public IP address.
                if(iface.getAssociation() != null) {
                    String publicIp = iface.getAssociation().getPublicIp();
                    LOG.debug("Network interface [{}] has public IP: {}", iface.getNetworkInterfaceId(), publicIp);
                    ipAddressesBuilder.put(publicIp, iface);
                }
            }

            ipAddresses = ipAddressesBuilder.build();

            // Load EC2 instances
            LOG.debug("Requesting EC2 instance descriptions.");
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
                    if(publicIp != null) {
                        LOG.debug("EC2 instance [{}] has public IP: {}", instance.getInstanceId(), publicIp);
                        ec2InstancesBuilder.put(publicIp, instance);
                    }
                }
            }

            ec2Instances = ec2InstancesBuilder.build();

            /*
            // Load ELB instances
            // TODO findLoadBalancerAttachedToInterface
            LOG.info("Requesting ELB instance descriptions.");
            AmazonElasticLoadBalancingClient elbClient = new AmazonElasticLoadBalancingClient(credentials);
            DescribeLoadBalancersResult elbResult = elbClient.describeLoadBalancers();
            for (LoadBalancerDescription elb : elbResult.getLoadBalancerDescriptions()) {
                LOG.info("Reading informaton for ELB instance [{}]", elb.getDNSName());
                lookupTableBuilder.put(elb.get);
            }*/

            this.loaded = true;
        } catch(Exception e) {
            LOG.error("Error when trying to refresh AWS instance lookup table.", e);
        }
    }

    public DiscoveredInstance findByIp(String ip) {
        try {
            // Let's see if this is an EC2 instance maybe?
            if (ec2Instances.containsKey(ip)) {
                Instance instance = ec2Instances.get(ip);
                LOG.debug("Found IP [{}] in EC2 instance lookup table.", ip);
                return new DiscoveredEC2Instance(instance.getInstanceId());
            }

            // Or maybe an ELB instance?
            // if()

            // The IP address is not known to us. This most likely means it is an external public IP.
            return DiscoveredInstance.UNDISCOVERED;
        } catch(Exception e) {
            LOG.error("Error when trying to match IP to AWS instance. Marking as undiscovered.", e);
            return DiscoveredInstance.UNDISCOVERED;
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
        String ownerId = iface.getAssociation().getIpOwnerId();

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

    public boolean isLoaded() {
        return loaded;
    }

}
