import * as React from "react";
import { mockTestStore } from "../../../utils/testUtils";
import { testRender } from "../../../shared-resources/components/testing/testing-react.wrapper";
import SmartTicketTemplateEditPage from "./smart-ticket-template-edit.page";
import * as pageSettingsActions from "reduxConfigs/actions/pagesettings.actions";
import { SET_PAGE_BUTTON_ACTION, SET_PAGE_SETTINGS, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import * as genericActions from "reduxConfigs/actions/restapi/generic.actions";
import * as actions from "reduxConfigs/actions/actionTypes";
describe("Smart Ticket Template Edit Page UI", () => {
  let testForm: any;
  let store: any;

  beforeAll(() => {
    store = mockTestStore();
  });

  test("Page should match the snapshot.", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: "random"
    };
    // @ts-ignore
    const { asFragment } = testRender(<SmartTicketTemplateEditPage location={mockLocation} />, {
      store
    });
    expect(asFragment()).toMatchSnapshot();
  });

  test("Summary field, if present should be disabled by default", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: "random"
    };
    // @ts-ignore
    const { container } = testRender(<SmartTicketTemplateEditPage location={mockLocation} />, {
      store
    });
    container.querySelectorAll("input").forEach(k => {
      if (k.value === "summary") {
        expect(k.disabled).toBeTruthy();
      }
    });
  });

  test("Assignee field, if present should be disabled by default", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: "random"
    };
    // @ts-ignore
    const { container } = testRender(<SmartTicketTemplateEditPage location={mockLocation} />, {
      store
    });
    container.querySelectorAll("input").forEach(k => {
      if (k.value === "assignee") {
        expect(k.disabled).toBeTruthy();
      }
    });
  });

  test("Tag field, if present should be enabled by default", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: "random"
    };
    // @ts-ignore
    const { container } = testRender(<SmartTicketTemplateEditPage location={mockLocation} />, {
      store
    });
    container.querySelectorAll("input").forEach(k => {
      if (k.value === "tags") {
        expect(k.disabled).toBeTruthy();
      }
    });
  });
});

describe("Smart Ticket Template Page Basic creation", () => {
  let store: any;
  const mockSetPageSettingsResponse: any = {
    type: SET_PAGE_SETTINGS
  };
  const mockSetPageButtonResponse: any = {
    type: SET_PAGE_BUTTON_ACTION
  };
  const mockGenericListResponse: any = { type: actions.RESTAPI_READ };
  let genericListSpy: any;
  let setPageSettingsSpy: any;
  let setPageButtonActionSpy: any;
  beforeEach(() => {
    store = mockTestStore();
    setPageSettingsSpy = jest
      .spyOn(pageSettingsActions, "setPageSettings")
      .mockReturnValue(mockSetPageSettingsResponse);
    setPageButtonActionSpy = jest
      .spyOn(pageSettingsActions, "setPageButtonAction")
      .mockReturnValue(mockSetPageButtonResponse);
    genericListSpy = jest.spyOn(genericActions, "genericList").mockReturnValue(mockGenericListResponse);
    // genericListSpy.mockReturnValue(mockGenericListResponse);
  });
  test("Should dispatch correct actions when component mounts", () => {
    const mockLocation = {
      pathname: "dummypathName",
      search: "random"
    };
    // @ts-ignore
    testRender(<SmartTicketTemplateEditPage location={mockLocation} />, {
      store
    });
    expect(store.dispatch).toHaveBeenCalledWith(mockSetPageSettingsResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockGenericListResponse);
    expect(store.dispatch).toHaveBeenCalledWith(mockSetPageButtonResponse);

    expect(genericListSpy).toHaveBeenCalledWith(
      "integrations",
      "list",
      {
        filter: { application: "slack" },
        page_size: 100
      },
      null,
      "slack"
    );

    expect(setPageSettingsSpy).toHaveBeenCalledWith("dummypathName", {
      action_buttons: {
        save: {
          disabled: true,
          hasClicked: false,
          icon: "save",
          label: "Save",
          type: "primary"
        },
        settings: { color: "red", hasClicked: false, icon: "setting", label: "Settings", type: "secondary" }
      },
      title: ""
    });

    expect(setPageButtonActionSpy).toHaveBeenCalledWith("dummypathName", "save", { disabled: true });
  });
});
