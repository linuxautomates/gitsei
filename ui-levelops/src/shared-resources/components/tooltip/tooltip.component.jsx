import React from 'react';
import * as PropTypes from 'prop-types';
import { Portal } from 'react-portal';
import { getWrapperBestPosition } from '../../lib';

import './tooltip.style.scss';

export class TooltipComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      visible: false,
      style: {},
      position: props.position,
    };

    this.elementRef = null;
    this.tooltipRef = null;
    this.onMouseEnterHandler = this.onMouseEnterHandler.bind(this);
    this.onMouseLeaveHandler = this.onMouseLeaveHandler.bind(this);
  }

  onMouseEnterHandler() {
    this.setState({
      visible: true,
    }, this.updatePosition);
  }

  onMouseLeaveHandler() {
    this.setState({
      visible: false,
    });
  }

  onSetRefHandler(element) {
    return (ref) => {
      this[element] = ref;
    };
  }

  updatePosition() {
    this.setState(getWrapperBestPosition(this.elementRef, this.tooltipRef, this.props.position));
  }

  render() {
    const { className } = this.props;

    return (
      <React.Fragment>
        <div
          ref={this.onSetRefHandler('elementRef')}
          onMouseEnter={this.onMouseEnterHandler}
          onMouseLeave={this.onMouseLeaveHandler}
          className={`${className}__trigger`}
        >
          {this.props.children}
        </div>
        {this.state.visible && (
          <Portal>
            <div
              ref={this.onSetRefHandler('tooltipRef')}
              className={`${className} ${className}--${this.state.position}`}
              style={this.state.style}
            >
              <div
                className={`${className}__content ${this.props.noWhiteSpace ? `${className}__content--no-white-space` : ''}`}>
                {this.props.tooltip}
              </div>
            </div>
          </Portal>
        )}
      </React.Fragment>
    );
  }
}

TooltipComponent.propTypes = {
  children: PropTypes.node.isRequired,
  tooltip: PropTypes.oneOfType([PropTypes.string, PropTypes.node]).isRequired,
  position: PropTypes.oneOf(['top', 'bottom', 'left', 'right']),
  noWhiteSpace: PropTypes.bool,
  className: PropTypes.string,
};

TooltipComponent.defaultProps = {
  className: 'mat-tooltip',
  position: 'bottom',
  noWhiteSpace: false,
};
