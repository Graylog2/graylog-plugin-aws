import React from 'react';
import { Input, Button } from 'react-bootstrap';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { IfPermitted, Select } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

const AWSPluginConfiguration = React.createClass({
    propTypes: {
        config: React.PropTypes.object,
        updateConfig: React.PropTypes.func.isRequired,
    },

    getDefaultProps() {
        return {
            config: {
                lookups_enabled: false,
                access_key: '',
                secret_key: '',
            },
        };
    },

    getInitialState() {
        return {
            config: ObjectUtils.clone(this.props.config),
        };
    },

    componentWillReceiveProps(newProps) {
        this.setState({config: ObjectUtils.clone(newProps.config)});
    },

    _updateConfigField(field, value) {
        const update = ObjectUtils.clone(this.state.config);
        update[field] = value;
        this.setState({config: update});
    },

    _onCheckboxClick(field, ref) {
        return () => {
            this._updateConfigField(field, this.refs[ref].getChecked());
        };
    },

    _onSelect(field) {
        return (selection) => {
            this._updateConfigField(field, selection);
        };
    },

    _onUpdate(field) {
        return (e) => {
            this._updateConfigField(field, e.target.value);
        };
    },

    _openModal() {
        this.refs.awsConfigModal.open();
    },

    _closeModal() {
        this.refs.awsConfigModal.close();
    },

    _resetConfig() {
        // Reset to initial state when the modal is closed without saving.
        this.setState(this.getInitialState());
    },

    _saveConfig() {
        this.props.updateConfig(this.state.config).then(() => {
            this._closeModal();
        });
    },

    render() {
        return (
            <div>
                <h3>AWS Plugin Configuration</h3>

                <p>
                    Base configuration for all plugins the AWS module is providing. Note that some parameters will be
                    stored in MongoDB without encryption. Graylog users with required permissions will be able to read
                    them in the configuration dialog on this page.
                </p>

                <dl className="deflist">
                    <dt>Instance detail lookups:</dt>
                    <dd>{this.state.config.lookups_enabled === true ? 'Enabled' : 'Disabled'}</dd>
                    <dt>Access Key:</dt>
                    <dd>{this.state.config.access_key ? "***********" : "[not set]" }</dd>

                    <dt>Secret Key:</dt>
                    <dd>{this.state.config.secret_key ? "***********" : "[not set]"}</dd>
                </dl>

                <IfPermitted permissions="clusterconfigentry:edit">
                    <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Configure</Button>
                </IfPermitted>

                <BootstrapModalForm ref="awsConfigModal"
                                    title="Update AWS Plugin Configuration"
                                    onSubmitForm={this._saveConfig}
                                    onModalClose={this._resetConfig}
                                    submitButtonText="Save">
                    <fieldset>
                        <Input type="checkbox"
                               ref="lookupsEnabled"
                               label="Run AWS instance detail lookups for IP addresses?"
                               help={<span>It can take up to a minute for a change of this to take effect.</span>}
                               name="lookups_enabled"
                               checked={this.state.config.lookups_enabled}
                               onChange={this._onCheckboxClick('lookups_enabled', 'lookupsEnabled')}/>

                        <Input type="text"
                               label="AWS Access Key"
                               help={<span>Note that this will only be used in encrypted connections but stored in plaintext.</span>}
                               name="access_key"
                               value={this.state.config.access_key}
                               onChange={this._onUpdate('access_key')}/>

                        <Input type="text"
                               label="AWS Secret Key"
                               help={<span>Note that this will only be used in encrypted connections but stored in plaintext.</span>}
                               name="secret_key"
                               value={this.state.config.secret_key}
                               onChange={this._onUpdate('secret_key')}/>
                    </fieldset>
                </BootstrapModalForm>
            </div>
        );
    },
});

export default AWSPluginConfiguration;