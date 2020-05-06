import 'webpack-entry';

import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import AWSPluginsConfig from 'components/AWSPluginConfiguration';
import packageJson from '../../package.json';
import { PLUGIN_PACKAGE } from './Constants';

PluginStore.register(new PluginManifest(packageJson, {
  systemConfigurations: [
    {
      component: AWSPluginsConfig,
      configType: PLUGIN_PACKAGE,
    },
  ],
}));
