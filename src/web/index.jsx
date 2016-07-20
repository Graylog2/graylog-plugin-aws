import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import AWSPluginsConfig from 'components/AWSPluginConfiguration';

PluginStore.register(new PluginManifest(packageJson, {
    systemConfigurations: [
        {
            component: AWSPluginsConfig,
            configType: 'org.graylog.aws.config.AWSPluginConfiguration',
        },
    ],
}));