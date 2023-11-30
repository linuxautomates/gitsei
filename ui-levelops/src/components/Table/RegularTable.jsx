import React, { Component } from "react";
import PropTypes from "prop-types";

class RegularTable extends Component {
  render() {
    return (
      <div className="table table-responsive">
        <table className="table table-hover">
          <thead className="text-center">
            <tr className="text-center">
              {this.props.th.map((prop, key) => {
                return (
                  <th key={key} className="text-center">
                    {prop.split("_").join(" ")}
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody align="left">
            {this.props.tds.map((row, key) => {
              return (
                <tr key={key}>
                  {row.map((col, key) => {
                    if (col === null) {
                      col = "";
                    }
                    if (!React.isValidElement(col)) {
                      if (Array.isArray(col)) {
                        // check if element of array is a valid react element
                        if (col.length > 0 && !React.isValidElement(col[0])) {
                          col = col.join(",");
                        }
                      } else if (col.constructor === Object) {
                        col = JSON.stringify(col);
                      }
                    }
                    return <td key={key}>{col}</td>;
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  }
}

RegularTable.propTypes = {
  th: PropTypes.array.isRequired,
  tds: PropTypes.arrayOf(PropTypes.array).isRequired
};

export default RegularTable;
