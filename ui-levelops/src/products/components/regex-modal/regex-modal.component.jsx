import React from "react";
import * as PropTypes from "prop-types";
import { Label, Modal } from "shared-resources/components";
import RegularTable from "components/Table/RegularTable";

const ths = ["field", "description"];
const tds = [
  [<Label type={"description"} text={"."} />, <Label type={"description"} text={"Any character except newline"} />],
  [<Label type={"description"} text={"a"} />, <Label type={"description"} text={"The character a"} />],
  [<Label type={"description"} text={"ab"} />, <Label type={"description"} text={"The string ab"} />],
  [<Label type={"description"} text={"a|b"} />, <Label type={"description"} text={"a or b"} />],
  [<Label type={"description"} text={"a*"} />, <Label type={"description"} text={"0 or more a"} />],
  [
    <Label type={"description"} text={"[ab-d]"} />,
    <Label type={"description"} text={"One character of: a, b, c, d\n"} />
  ],
  [
    <Label type={"description"} text={"[^ab-d]"} />,
    <Label type={"description"} text={"One character except: a, b, c, d"} />
  ],
  [<Label type={"description"} text={"[\\b]"} />, <Label type={"description"} text={"Backspace character"} />],
  [<Label type={"description"} text={"\\d"} />, <Label type={"description"} text={"One digit"} />],
  [<Label type={"description"} text={"\\D"} />, <Label type={"description"} text={"One non digit"} />],
  [<Label type={"description"} text={"\\s"} />, <Label type={"description"} text={"One white space"} />],
  [<Label type={"description"} text={"\\S"} />, <Label type={"description"} text={"One non white space"} />],
  [<Label type={"description"} text={"\\w"} />, <Label type={"description"} text={"One word character"} />],
  [<Label type={"description"} text={"\\S"} />, <Label type={"description"} text={"One non word character"} />]
];

export class RegexModalComponent extends React.PureComponent {
  render() {
    return (
      <Modal title={"Regex Guide"} onCloseEvent={this.props.onCloseEvent}>
        <RegularTable th={ths} tds={tds} />
      </Modal>
    );
  }
}

RegexModalComponent.propTypes = {
  onCloseEvent: PropTypes.func.isRequired
};

RegexModalComponent.defaultProps = {};
