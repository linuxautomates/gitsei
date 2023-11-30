import React from 'react';
import * as PropTypes from 'prop-types';
import { SvgIconComponent as SvgIcon } from "shared-resources/components/svg-icon/svg-icon.component";
import './avatar.style.scss';

export class AvatarComponent extends React.PureComponent {
  constructor(props) {
    super(props);
  }

  render() {
    const { className } = this.props;

    return (
      <div
        className={`centered ${className} ${className}__${this.props.size}`}
        onClick={this.props.onClickEvent}
        role="presentation"
      >
        {!!this.props.avatar.length && <img src={this.props.avatar} alt="avatar"/>}
        {!this.props.avatar.length && <SvgIcon icon="avatar"/>}
      </div>
    );
  }
}

AvatarComponent.propTypes = {
  className: PropTypes.string,
  avatar: PropTypes.string,
  onClickEvent: PropTypes.func,
  size: PropTypes.oneOf(['small', 'medium', 'large']),
};

AvatarComponent.defaultProps = {
  className: 'avatar',
  avatar: '',
  onClickEvent: () => {
  },
  size: 'medium',
};
