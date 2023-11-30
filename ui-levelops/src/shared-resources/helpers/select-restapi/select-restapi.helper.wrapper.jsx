import uuidv1 from "uuid/v1";
import React from "react";
import { default as SelectRestapi } from "./select-restapi.helper";

export class SelectRestapiHelperWrapper extends React.Component {
  // the main reason is because createselector does not consider default props
  constructor(props) {
    super(props);
    this.state = {
      uuid: uuidv1()
    };
  }

  render() {
    return (
      <SelectRestapi
        {...this.props}
        innerRef={this.props.innerRef}
        uuid={this.props.uuid ? this.props.uuid : this.state.uuid}
      />
    );
  }
}
