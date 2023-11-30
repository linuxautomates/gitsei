import * as actions from "../actionTypes";

export const notesList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: "notes",
  function: "getNotes",
  method: "list"
});

export const notesCreate = (note, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: note,
  uri: "notes",
  id: id,
  function: "createNote",
  method: "create"
});
