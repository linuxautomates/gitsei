import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { get } from "lodash";

const productsRestState = state => get(state.restapiReducer, ["products", "list", 0, "data", "records"], []);

export const getAllTeamsSelector = createSelector(productsRestState, products => {
  if (!products.length) {
    return [];
  }

  return products.map(product => ({
    id: product.id,
    name: product.name
  }));
});

export const productsState = createSelector(restapiState, state => {
  return get(state, ["products"], {});
});

export const productsGetState = createSelector(productsState, state => {
  return get(state, ["get"], { loading: true, error: false });
});
