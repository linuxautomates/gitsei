import { NO_SCOPE } from "../layouts.constants";
import {
  isAccountSelectedFromNoScopeState,
  isNoScopeActive,
  isProjectSelectedFromNoScopeState
} from "./../layout.utils";

describe("isNoScopeActive", () => {
  it("should return true when searchParam matches NO_SCOPE", () => {
    const searchParam = NO_SCOPE;
    const result = isNoScopeActive(searchParam);
    expect(result).toBe(true);
  });

  it("should return false when searchParam does not match NO_SCOPE", () => {
    const searchParam = "some_other_value";
    const result = isNoScopeActive(searchParam);
    expect(result).toBe(false);
  });
});

describe("isProjectSelectedFromNoScopeState", () => {
  it("should return true when all conditions are met", () => {
    const currentLocation = "https://example.com/projects/orgs/home?some_path";
    const result = isProjectSelectedFromNoScopeState(currentLocation, true);
    expect(result).toBe(true);
  });

  it("should return false when any condition is not met", () => {
    const currentLocation = "https://example.com/account/home?some_path";
    const result = isProjectSelectedFromNoScopeState(currentLocation, true);
    expect(result).toBe(false);
  });

  it("should return false when isNav2Enabled is falsy", () => {
    const currentLocation = "https://example.com/projects/orgs/home?some_path";
    const result = isProjectSelectedFromNoScopeState(currentLocation, false);
    expect(result).toBe(false);
  });
});

describe("isAccountSelectedFromNoScopeState", () => {
  it("should return true when all conditions are met", () => {
    const currentLocation = "https://example.com/account/home?some_path";
    const result = isAccountSelectedFromNoScopeState(currentLocation, true);
    expect(result).toBe(true);
  });

  it("should return false when any condition is not met", () => {
    const currentLocation = "https://example.com/projects/orgs/home?some_path";
    const result = isAccountSelectedFromNoScopeState(currentLocation, true);
    expect(result).toBe(false);
  });

  it("should return false when isNav2Enabled is falsy", () => {
    const currentLocation = "https://example.com/account/home?some_path";
    const result = isAccountSelectedFromNoScopeState(currentLocation, false);
    expect(result).toBe(false);
  });
});
