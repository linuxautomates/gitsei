import React from 'react';
import * as PropTypes from 'prop-types';
import { Portal } from 'react-portal';

import './dom-wrapper.style.scss';

export class DOMWrapperComponent extends React.PureComponent {
  componentDidMount() {
    document.body.style.overflow = 'hidden';
    if (document.body.offsetHeight > window.innerHeight) {
      document.body.style.paddingRight = '15px';
    }
  }

  componentWillUnmount() {
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  }

  render() {
    const { className } = this.props;

    return (
      <Portal>
        <div className={`${className} ${className}--${this.props.skin}`}>
          <div
            className={`${className}__overlay`}
            onClick={this.props.onClose}
            role="presentation"
          />
          {this.props.children}
        </div>
      </Portal>
    );
  }
}

DOMWrapperComponent.propTypes = {
  onClose: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  skin: PropTypes.oneOf(['modal', 'default', 'fullscreen']),
  className: PropTypes.string,
};

DOMWrapperComponent.defaultProps = {
  className: 'dom-wrapper',
  skin: 'default',
};
