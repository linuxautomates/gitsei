import { createSelector } from "reselect";
import { get } from "lodash";
import { restapiState } from "./restapiSelector";

export const quizState = createSelector(restapiState, (state: any) => {
  return get(state, ["quiz"], {});
});

export const quizDeleteState = createSelector(quizState, (state: any) => {
  return get(state, ["delete"], {});
});

export const quizGetState = createSelector(quizState, (state: any) => {
  return get(state, ["get"], {});
});

export const quizUpdateState = createSelector(quizState, (state: any) => {
  return get(state, ["update"], {});
});

export const quizFilesUploadState = createSelector(quizState, (state: any) => {
  return get(state, ["upload"], {});
});

export const quizNotifyState = createSelector(restapiState, (state: any) => {
  return get(state, ["questionnaires_notify", "list"], {});
});

export const quizDeleteSelector = createSelector(quizDeleteState, (data: any) => data);
