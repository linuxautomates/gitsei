import { createSelector } from "reselect";

const teamsRestState = state => get(state.restapiReducer, ["teams", "list", 0, "data", "records"], []);

export const getAllTeamsSelector = createSelector(teamsRestState, teams => {
  if (!teams.length) {
    return [];
  }

  return teams.map(team => ({
    id: team.id,
    name: team.name
  }));
});
