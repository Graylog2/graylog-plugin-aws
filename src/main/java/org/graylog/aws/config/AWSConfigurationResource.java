package org.graylog.aws.config;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "AWS/Config", description = "Manage AWS Config settings")
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class AWSConfigurationResource extends RestResource implements PluginRestResource {
    private final ClusterConfigService clusterConfigService;
    private final Configuration systemConfiguration;

    @Inject
    public AWSConfigurationResource(ClusterConfigService clusterConfigService,
                                    Configuration systemConfiguration) {
        this.clusterConfigService = clusterConfigService;
        this.systemConfiguration = systemConfiguration;
    }

    @PUT
    @ApiOperation(value = "Returns all existing archives for the given backend")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_UPDATE)
    public Response updateConfig(@Valid AWSPluginConfigurationUpdate update) {
        final AWSPluginConfiguration existingConfiguration = clusterConfigService.getOrDefault(
                AWSPluginConfiguration.class,
                AWSPluginConfiguration.createDefault()
        );
        final AWSPluginConfiguration.Builder newConfigBuilder = existingConfiguration.toBuilder()
                .lookupsEnabled(update.lookupsEnabled())
                .lookupRegions(update.lookupRegions())
                .accessKey(update.accessKey())
                .proxyEnabled(update.proxyEnabled());

        final AWSPluginConfiguration newConfiguration = update.secretKey()
                .map(secretKey -> newConfigBuilder.secretKey(secretKey, systemConfiguration.getPasswordSecret()))
                .orElse(newConfigBuilder)
                .build();

        clusterConfigService.write(newConfiguration);
        return Response.accepted(newConfiguration).build();
    }
}
