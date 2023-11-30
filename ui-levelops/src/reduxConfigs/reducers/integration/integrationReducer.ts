import { createReducer } from "../../createReducer";
import { integrationHandler } from "./handlers";

const cachedIntegrationReducer = createReducer({}, integrationHandler);

export default cachedIntegrationReducer;
