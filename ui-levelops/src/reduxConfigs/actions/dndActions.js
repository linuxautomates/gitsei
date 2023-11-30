import { ADD_DASHBOARD_DRAG, ADD_DASHBOARD_DROP, CLEAR_DASHBOARD_DRAG, CLEAR_DASHBOARD_DROP } from "./actionTypes";

export const addDashboardDrag = id => ({ type: ADD_DASHBOARD_DRAG, payload: id });

export const addDashboardDrop = id => ({ type: ADD_DASHBOARD_DROP, payload: id });

export const clearDashboardDrag = () => ({ type: CLEAR_DASHBOARD_DRAG });

export const clearDashboardDrop = () => ({ type: CLEAR_DASHBOARD_DROP });
