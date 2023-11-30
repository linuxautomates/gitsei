import React from "react";
import * as PropTypes from "prop-types";
import "./lql-array.style.scss";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import LQL from "components/LQL/LQL";
import { PREDICATES } from "constants/queryPredicates";

export class LQLArrayComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.handleRemove = this.handleRemove.bind(this);
    this.updateQuery = this.updateQuery.bind(this);
  }

  handleRemove(index) {
    return e => {
      this.props.onDelete(index);
    };
  }

  updateQuery(index) {
    return (query, result, id = 0) => {
      this.props.onChange(query, index, result);
    };
  }

  render() {
    const { className } = this.props;

    let LQLs = this.props.lqls;
    console.log(LQLs);
    //LQLs.push("");
    return (
      <div style={{ paddingTop: "10px" }}>
        {/* eslint-disable-next-line array-callback-return */}
        {LQLs.map((lql, index) => {
          if (lql !== undefined) {
            return (
              <div className={`flex direction-row justify-space-between`} style={{ paddingBottom: "10px" }}>
                <div className={`${className}__lql-bar`}>
                  <LQL id={`lql-${index}`} query={lql} onChange={this.updateQuery(index)} predicates={PREDICATES} />
                </div>
                <table>
                  <tbody>
                    <tr>
                      <td>
                        <AntButton
                          className="ant-btn-outline mx-5"
                          icon={"delete"}
                          disabled={LQLs.length === 1}
                          onClick={this.handleRemove(index)}
                        />
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            );
          }
        })}
        <a
          // eslint-disable-next-line jsx-a11y/anchor-is-valid
          href={"#"}
          onClick={e => {
            e.preventDefault();
            this.props.onAdd(e);
          }}
          className={`${className}__add-label`}>
          Add LQL
        </a>
        {/*<IconButton icon="plus"*/}
        {/*            onClickEvent={this.props.onAdd}*/}
        {/*            style={{ width: '1.4rem', height: '1.4rem' }}*/}
        {/*/>*/}
      </div>
    );
  }
}

LQLArrayComponent.propTypes = {
  className: PropTypes.string,
  lqls: PropTypes.array.isRequired,
  onAdd: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired
};

LQLArrayComponent.defaultProps = {
  className: "lql-array-helper"
};
