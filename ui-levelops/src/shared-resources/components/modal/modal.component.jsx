import React from 'react';
import * as PropTypes from 'prop-types';
import { DOMWrapperComponent as DOMWrapper } from "shared-resources/helpers/dom-wrapper/dom-wrapper.component";
import { IconButtonComponent as IconButton } from "shared-resources/components/icon-button/icon-button.component";
import './modal.style.scss';

export class ModalComponent extends React.PureComponent {
  render() {
    const { className, skin } = this.props;

    return (
      <DOMWrapper
        skin={skin}
        onClose={this.props.onCloseEvent}
      >
        <div
          className={`${className} ${className}--${skin} ${this.props.extraClassName}`}
          style={this.props.style}
        >
          {skin === 'fullscreen' && (
            <div className={`${className}__header flex`}>
              <IconButton
                icon="close"
                onClickEvent={this.props.onCloseEvent}
                style={{
                  width: 22,
                  height: 22,
                }}
              />
              <div className={`${className}__title`}>{this.props.title}</div>
            </div>
          )}
          {skin === 'modal' && (
            <div className={`${className}__title flex`}>
              <IconButton
                icon="close"
                onClickEvent={this.props.onCloseEvent}
                style={{
                  width: 22,
                  height: 22,
                }}
              />
              <div className={`${className}__title--text`}>{this.props.title}</div>
            </div>
          )}
          <div className={`${className}__body`}>
            {this.props.children}
          </div>
        </div>
      </DOMWrapper>
    );
  }
}

ModalComponent.propTypes = {
  className: PropTypes.string,
  skin: PropTypes.oneOf(['fullscreen', 'modal']),
  children: PropTypes.any,
  style: PropTypes.object,
  onCloseEvent: PropTypes.func.isRequired,
  title: PropTypes.string,
  extraClassName: PropTypes.string,
};

ModalComponent.defaultProps = {
  className: 'dom-modal',
  skin: 'modal',
  children: '',
  style: {},
  title: '',
  extraClassName: '',
};
