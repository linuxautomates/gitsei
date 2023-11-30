import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { AntCard, AntInput, AntForm, AntFormItem } from "shared-resources/components";
import { mapProductFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { validateEmail } from "utils/stringUtils";
import { EMAIL_WARNING, EMPTY_FIELD_WARNING, ERROR } from "../../../constants/formWarnings";

export class ProductInformationContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {};
    this.onChangeHandler = this.onChangeHandler.bind(this);
  }

  onChangeHandler(field) {
    return e => {
      let product = Object.assign(
        Object.create(Object.getPrototypeOf(this.props.product_form)),
        this.props.product_form
      );
      switch (field) {
        case "name":
          product.name = e.currentTarget.value;
          break;
        case "description":
          product.description = e.currentTarget.value;
          break;
        case "owner":
          product.owner = e.currentTarget.value;
          break;
        default:
          break;
      }
      //product[field] = value;
      this.props.formUpdateObj("product_form", product);
    };
  }

  render() {
    const { style, product_form } = this.props;
    return (
      <AntCard style={style}>
        <AntForm layout={"vertical"}>
          <AntFormItem
            label={"Name"}
            colon={false}
            required={true}
            //help={"This field cannot be blank"}
            validateStatus={this.state.name && product_form.name === "" ? ERROR : ""}
            hasFeedback={true}
            help={this.state.name && product_form.name === "" && EMPTY_FIELD_WARNING}>
            <AntInput
              name={"name"}
              value={product_form.name}
              onChange={this.onChangeHandler("name")}
              onBlur={e => {
                this.setState({
                  name: true
                });
              }}
            />
          </AntFormItem>
          <AntFormItem label={"Description"} colon={false} required={false} hasFeedback={false}>
            <AntInput
              name={"description"}
              value={product_form.description}
              onChange={this.onChangeHandler("description")}
            />
          </AntFormItem>
          <AntFormItem
            label={"Owner"}
            colon={false}
            required={true}
            hasFeedback={true}
            help={this.state.owner && !validateEmail(product_form.owner) && EMAIL_WARNING}
            validateStatus={this.state.owner && !validateEmail(product_form.owner) ? ERROR : ""}>
            <AntInput
              name={"owner"}
              value={product_form.owner}
              onChange={this.onChangeHandler("owner")}
              placeholder={"productowner@acme.io"}
              //help={"Valid email needed"}
              onBlur={e => {
                this.setState({
                  owner: true
                });
              }}
            />
          </AntFormItem>
        </AntForm>
      </AntCard>
    );
  }
}

ProductInformationContainer.propTypes = {
  className: PropTypes.string,
  style: PropTypes.object,
  product_form: PropTypes.object
};

ProductInformationContainer.defaultProps = {
  className: "",
  style: {}
};

export default connect(mapProductFormStatetoProps, mapFormDispatchToPros)(ProductInformationContainer);
