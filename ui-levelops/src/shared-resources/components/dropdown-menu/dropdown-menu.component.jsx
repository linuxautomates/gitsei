import React from 'react';
import * as PropTypes from 'prop-types';

import { DropdownWrapperHelper } from 'shared-resources/helpers';
import { Option } from 'shared-resources/components';

import './dropdown-menu.style.scss';

export class DropdownMenuComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.onOptionClickHandler = this.onOptionClickHandler.bind(this);
  }

  onOptionClickHandler(option) {
    this.props.onOptionClickEvent(option);
    this.props.onCloseMenuHandler();
  }

  render() {
    const { className } = this.props;
    return (
      <div className={className}>
        <DropdownWrapperHelper
          triggerElement={this.props.triggerElement}
          onClose={this.props.onCloseMenuHandler}
        >
          <div
            className={`${className}__options`}
          >
            {this.props.options.map(option => (
              <Option
                key={`option-${option.id}`}
                onClickEvent={this.onOptionClickHandler}
                option={option}
              />
            ))
            }

          </div>
        </DropdownWrapperHelper>
      </div>
    );
  }
}

DropdownMenuComponent.propTypes = {
  className: PropTypes.string,
  options: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired,
    }),
  ).isRequired,
  triggerElement: PropTypes.object.isRequired,
  onOptionClickEvent: PropTypes.func.isRequired,
  onCloseMenuHandler: PropTypes.func.isRequired,
};

DropdownMenuComponent.defaultProps = {
  className: 'dropdown-menu',
};
