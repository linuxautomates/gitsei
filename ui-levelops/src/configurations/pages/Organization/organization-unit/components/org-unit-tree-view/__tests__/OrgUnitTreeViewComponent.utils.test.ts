import { getIsDisabled } from "../OrgUnitTreeViewComponent.utils";
import { getIsStandaloneApp } from "../../../../../../../helper/helper";

jest.mock("helper/helper");
(getIsStandaloneApp as jest.Mock).mockImplementation(() => true);

describe("OrgUnitTreeViewComponent.utils tests", () => {
  test("getIsDisabled, when standalone", () => {
    (getIsStandaloneApp as jest.Mock).mockImplementation(() => true);
    expect(getIsDisabled({}, false)).toBeFalsy();
    expect(getIsDisabled({ key: "abc" }, false, [{ id: "abc" }])).toBeFalsy();
    expect(getIsDisabled({ key: "abc" }, false, [{ id: "222" }])).toBeFalsy();
    expect(getIsDisabled({ key: "abc" }, undefined, [{ id: "222" }])).toBeTruthy();
    expect(getIsDisabled({ key: "abc" }, undefined, undefined, ["123"])).toBeTruthy();
  });
  test("getIsDisabled, when standalone", () => {
    (getIsStandaloneApp as jest.Mock).mockImplementation(() => false);
    expect(
      getIsDisabled(
        {
          access_response: {
            view: true
          }
        },
        false
      )
    ).toBeFalsy();
    expect(
      getIsDisabled(
        {
          access_response: {
            view: false
          }
        },
        false
      )
    ).toBeTruthy();
  });
});
