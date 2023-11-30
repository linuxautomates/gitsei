import React from "react";
import * as PropTypes from "prop-types";
import { Icon, Avatar, Tooltip } from "antd";
import { connect } from "react-redux";
import ErrorWrapper from "hoc/errorWrapper";
import { AntCard, InfiniteScroll } from "shared-resources/components";
import { mapPaginationStatetoProps } from "reduxConfigs/maps/paginationMap";
import "./products-infinite-list-card.style.scss";

export class ProductsInfiniteListCardContainer extends React.PureComponent {
  componentDidUpdate(prevProps, prevState, snapshot) {
    if (this.props.pagination_data.length && this.props.pagination_data !== prevProps.pagination_data) {
      const firstProduct = this.props.pagination_data[0];
      if (firstProduct.id) {
        this.props.onSelectProductEvent(firstProduct.id);
      }
    }
  }

  onSelectProductHandler(id) {
    return () => {
      this.props.onSelectProductEvent(id);
    };
  }

  getGrade(score) {
    const gradeColors = {
      A: "#37b47e",
      B: "#37b47e",
      C: "#ffac00",
      D: "#ffac00",
      F: "#ff5630"
    };
    let grade = "A";
    if (score < 70 && score >= 60) {
      grade = "B";
    } else if (score < 60 && score >= 50) {
      grade = "C";
    } else if (score > 50 && score <= 45) {
      grade = "D";
    } else if (score < 45) {
      grade = "F";
    }
    return (
      <Avatar
        size={"large"}
        style={{ backgroundColor: gradeColors[grade], color: "#ffffff", fontSize: "16px", fontWeight: "bold" }}>
        {grade}
      </Avatar>
    );
  }

  render() {
    const mappedProducts = this.props.pagination_data.map((product, index) => ({
      ...product,
      score: product.score || 0,
      progress: product.progress || 100 / index,
      trend: product.trend || 45,
      isSelected: product.id === this.props.selected
    }));

    return (
      <div className="products-infinite-list">
        <InfiniteScroll horizontal restMethod="getProducts" uri="products" pageSize={10}>
          {mappedProducts.map(product => (
            <AntCard
              className={product.isSelected ? "product selected" : "product"}
              key={`product-${product.id}`}
              bordered={!product.isSelected}
              onClickEvent={this.onSelectProductHandler(product.id)}>
              <div className="flex direction-column product-data">
                <div className="flex direction-row justify-space-between align-center product-data__title">
                  <Tooltip title={product.name}>
                    <h3 className="medium-16 ellipsis" style={{ width: "70%" }}>
                      {product.name}
                    </h3>
                  </Tooltip>
                  {this.getGrade(parseInt(product.score))}
                </div>
                <div className="flex align-center product-data__extra">
                  <Icon
                    type={product.trend > 0 ? "caret-up" : "caret-down"}
                    style={{ fontSize: "8px", color: product.trend > 0 ? "#37b47e" : "#ff5630" }}
                  />
                  <span className="info1">
                    {product.trend > 0 ? `+${product.trend}` : product.trend}% since last month{" "}
                  </span>
                </div>
              </div>
            </AntCard>
          ))}
        </InfiniteScroll>
      </div>
    );
  }
}

ProductsInfiniteListCardContainer.propTypes = {
  onSelectProductEvent: PropTypes.func.isRequired
};

export default ErrorWrapper(connect(mapPaginationStatetoProps, null)(ProductsInfiniteListCardContainer));
