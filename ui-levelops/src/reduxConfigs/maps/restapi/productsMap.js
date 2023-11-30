import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapProductsToProps = dispatch => {
  return {
    productsCreate: item => dispatch(actionTypes.productsCreate(item)),
    productsDelete: id => dispatch(actionTypes.productsDelete(id)),
    productsUpdate: (id, item) => dispatch(actionTypes.productsUpdate(id, item)),
    productsGet: id => dispatch(actionTypes.productsGet(id)),
    productsList: (filters, id = "0") => dispatch(actionTypes.productsList(filters, id)),
    productsBulk: filters => dispatch(actionTypes.productsBulk(filters))
  };
};
