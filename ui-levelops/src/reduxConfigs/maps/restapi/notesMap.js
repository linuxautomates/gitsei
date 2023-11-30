import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapNotesToProps = dispatch => {
  return {
    notesCreate: (note, id = "0") => dispatch(actionTypes.notesCreate(note, id)),
    notesList: filters => dispatch(actionTypes.notesList(filters))
  };
};
