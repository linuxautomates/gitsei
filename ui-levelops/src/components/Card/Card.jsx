import React, { Component } from "react";

class Card extends Component {
  render() {
    const emptyFunction = () => {};
    let crossButton =
        <div className="stats">
        <button
            type="button"
            aria-hidden="true"
            className="close"
            name={this.props.closeName? this.props.closeName:""}
            onClick={this.props.closeOnClick? this.props.closeOnClick:emptyFunction}
        >
        ×
        </button></div>;
    return (
      <div
        className={
          "card" +
          (this.props.hidden ? " card-hidden" : "") +
          (this.props.calendar ? " card-calendar" : "") +
          (this.props.plain ? " card-plain" : "") +
          (this.props.wizard ? " card-wizard" : "")
        }
      >

        {this.props.title !== undefined || this.props.category !== undefined ? (
          <div
            className={"header" + (this.props.textCenter ? " text-center" : "")}
          >
              {this.props.close? crossButton:""}
            <h4 className="title">{this.props.title}</h4>
            <p className="category">{this.props.category}</p>
          </div>
        ) : (
          ""
        )}
        <div
          className={
            "content" +
            (this.props.ctAllIcons ? " all-icons" : "") +
            (this.props.ctFullWidth ? " content-full-width" : "") +
            (this.props.ctTextCenter ? " text-center" : "") +
            (this.props.tableFullWidth ? " table-full-width" : "") +
            (this.props.noPadding? " content-no-padding": "")
          }
        >
          {this.props.content}
        </div>
        {this.props.stats !== undefined || this.props.legend !== undefined ? (
          <div
            className={
              "footer" + (this.props.ftTextCenter ? " text-center" : "")
            }
          >
            {this.props.legend !== undefined ? (
              <div className="legend">{this.props.legend}</div>
            ) : null}
            {this.props.stats !== undefined ? <hr /> : null}
            {this.props.stats !== undefined ? (
              <div className="stats">{this.props.stats}</div>
            ) : null}
          </div>
        ) : null}
      </div>
    );
  }
}

export default Card;
