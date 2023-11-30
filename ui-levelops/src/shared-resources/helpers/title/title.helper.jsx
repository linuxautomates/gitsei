import React from "react";
import * as PropTypes from "prop-types";

import "./title.style.scss";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";

export class TitleComponent extends React.PureComponent {
  constructor(props) {
    super(props);
  }

  render() {
    const { className, onClick, title, buttonTitle, button, buttonDisabled } = this.props;
    return (
      <div>
        <div className={`${className} flex direction-row justify-space-between align-bottom`}>
          <div className={`${className} ${className}__title`}>{title}</div>
          {button && (
            <div className={`${className} ${className}__button`}>
              <AntButton type="primary" onClick={onClick} disabled={buttonDisabled}>
                {buttonTitle}
              </AntButton>
            </div>
          )}
        </div>
      </div>
    );
  }
}

TitleComponent.propTypes = {
  className: PropTypes.string,
  onClick: PropTypes.func,
  title: PropTypes.string.isRequired,
  buttonTitle: PropTypes.string,
  button: PropTypes.bool,
  buttonDisabled: PropTypes.bool
};

TitleComponent.defaultProps = {
  className: "title-helper",
  onClick: e => {},
  button: true,
  buttonDisabled: false
};
