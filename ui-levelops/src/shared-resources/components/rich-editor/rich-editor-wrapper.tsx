import React, { useState, useEffect } from "react";
import { Editor } from "@tinymce/tinymce-react";
import "./rich-editor-wrapper.style.scss";
import { useSelector } from "react-redux";
import { useParams } from "react-router-dom";
import { get } from "lodash";
import envConfig from "env-config";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";
import { createSelector } from "reselect";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";

export const EDITOR_API_KEY = envConfig.get("TINYMCE_API_KEY");

interface RichEditorProps {
  onChange: (event: any) => void;
  initialValue?: string;
  value?: string;
}

const INITIAL_VALUE = "<p>Start writing</p>";

const TOOL_BAR =
  "undo redo | formatselect | bold italic forecolor backcolor emoticons link | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | removeformat | help";

const PLUGINS = "lists link code textpattern help emoticons";
const dashboardsListSelector = createSelector(restapiState, (dashboards: any) => {
  return get(dashboards, ["dashboards", "list"], {});
});

const RichEditorComponent: React.FC<RichEditorProps> = ({ onChange, initialValue = INITIAL_VALUE, value }) => {
  const [linkList, setLinkList] = useState([]);
  const projectParams = useParams<ProjectPathProps>();

  const dashboardListState = useSelector(dashboardsListSelector);

  useEffect(() => {
    const loading = get(dashboardListState, ["DROPDOWN_DASH", "loading"], true);

    if (!loading) {
      const records = get(dashboardListState, ["DROPDOWN_DASH", "data", "records"], []);

      const list = records.map((item: any) => ({
        title: item.name.substr(0, 55) + (item.name.length > 55 ? "..." : ""),
        value: window.location.origin + getDashboardsPage(projectParams) + item.id
      }));
      list.length !== linkList.length && setLinkList(list);
    }
  }, [dashboardListState]);

  return (
    <div className="levelops-rich-editor">
      <Editor
        apiKey={EDITOR_API_KEY}
        initialValue={initialValue}
        init={{
          plugins: PLUGINS,
          menubar: false,
          toolbar: TOOL_BAR,
          height: 250,
          width: 800,
          branding: false,
          content_css: false,
          body_class: "rich-editor-body",
          content_style: "p {margin: 5px} a { color: #2967dd; text-decoration: none }",
          statusbar: false,
          link_list: linkList,
          link_title: false,
          dialog_type: "modal",
          default_link_target: "_blank"
        }}
        value={value}
        onEditorChange={onChange}
      />
    </div>
  );
};

export default React.memo(RichEditorComponent);
