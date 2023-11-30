import { createReducer } from "../../createReducer";
import { workspaceHandler } from "./handlers";

const workSpaceReducer = createReducer({}, workspaceHandler);

export default workSpaceReducer;
