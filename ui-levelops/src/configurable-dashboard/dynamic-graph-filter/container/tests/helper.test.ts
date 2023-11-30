import { buildDataForFilterApiCalling } from "../helper";

describe("dynamicGraphFilterHelpers", () => {
  test("buildDataForFilterApiCalling should return array", () => {
    expect(
      buildDataForFilterApiCalling("levelops_workitem_count_report", {
        across: "state",
        tags: ["448", "172", "167"],
        states: [248, 6]
      })
    ).toEqual({ state_ids: [248, 6], tag_ids: ["448", "172", "167"] });
  });
});
