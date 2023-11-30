import React from "react";
import * as PropTypes from "prop-types";
import { IconButtonComponent as IconButton } from "shared-resources/components/icon-button/icon-button.component";
import { OptionComponent as Option } from "shared-resources/components/option/option.component";
import "./card.style.scss";
import { DropdownWrapperHelper } from "../../helpers/dropdown-wrapper/dropdown-wrapper.helper";

export class CardComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      isMenuOpened: false
    };

    this.triggerElement = null;
    this.onToggleMenuHandler = this.onToggleMenuHandler.bind(this);
    this.setTriggerElement = this.setTriggerElement.bind(this);
    this.getClasses = this.getClasses.bind(this);
  }

  onToggleMenuHandler() {
    this.setState(state => ({
      isMenuOpened: !state.isMenuOpened
    }));
  }

  getClasses() {
    const { className } = this.props;
    const classes = [className];
    if (this.props.selected) {
      classes.push(`${className}--selected`);
    }

    return classes.join(" ");
  }

  setTriggerElement(ref) {
    this.triggerElement = ref;
  }

  render() {
    const { className } = this.props;

    return (
      <div className={this.getClasses()} onClick={this.props.onClickEvent} style={this.props.style}>
        <div className="flex justify-space-between">
          {this.props.title && <div className={`${className}__title`}>{this.props.title}</div>}
          {this.props.hasActions && (
            <div ref={this.setTriggerElement} style={{ flexBasis: "15%" }}>
              <IconButton rotateIcon icon="more" onClickEvent={this.onToggleMenuHandler} />
            </div>
          )}
        </div>
        {this.props.hasActions && <div className={`${className}__actions`} />}
        <div className={`${className}__body`}>{this.props.children}</div>

        {this.state.isMenuOpened && (
          <DropdownWrapperHelper triggerElement={this.triggerElement} onClose={this.onToggleMenuHandler}>
            <div className="card-menu">
              {this.props.menuOptions.map(option => (
                <Option
                  key={option.id}
                  onClickEvent={id => {
                    this.onToggleMenuHandler();
                    option.onClickEvent(id);
                  }}
                  option={option}
                />
              ))}
            </div>
          </DropdownWrapperHelper>
        )}
      </div>
    );
  }
}

CardComponent.propTypes = {
  className: PropTypes.string,
  title: PropTypes.any,
  hasActions: PropTypes.bool,
  menuOptions: PropTypes.array,
  onClickEvent: PropTypes.func,
  selected: PropTypes.bool,
  style: PropTypes.object
};

CardComponent.defaultProps = {
  className: "card",
  style: {},
  title: "",
  hasActions: false,
  menuOptions: [],
  onClickEvent: () => {},
  selected: false
};
