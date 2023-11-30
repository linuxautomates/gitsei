import React from "react";
import { connect } from "react-redux";
import { TableRowActions } from "shared-resources/components";
import Loader from "components/Loader/Loader";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import ErrorWrapper from "hoc/errorWrapper";
import { ServerPaginatedTable } from "shared-resources/containers";
import { getLoading } from "utils/loadingUtils";
import { getSettingsPage } from 'constants/routePaths'
import { tableColumns } from "./table-config";

export class QuestionsListPage extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      search_term: "",
      more_filters: {},
      partial_filters: {},
      tags_select: {},
      delete_loading: false,
      delete_section_id: undefined,
      highlighted_row: undefined
    };

    this.onRemoveHandler = this.onRemoveHandler.bind(this);
    this.onEditHandler = this.onEditHandler.bind(this);
    this.onRowClick = this.onRowClick.bind(this);
  }

  componentWillReceiveProps(nextProps, nextContext) {
    if (this.state.delete_loading) {
      if (!getLoading(nextProps.rest_api, "sections", "delete", this.state.delete_section_id.toString())) {
        this.setState({
          delete_loading: false,
          delete_section_id: undefined
        });
      }
    }
  }

  componentWillUnmount() {
    this.props.restapiClear("sections", "list", "0");
    this.props.restapiClear("sections", "delete", "-1");
    this.props.restapiClear("tags", "list", "0");
    this.props.restapiClear("tags", "bulk", "0");
  }

  onEditHandler(questionId) {
    this.props.history.push(`${getSettingsPage()}/edit-question-page?section=${questionId}`);
  }

  onRemoveHandler(sectionId) {
    this.setState(
      {
        delete_loading: true,
        delete_section_id: sectionId
      },
      () => this.props.sectionsDelete(sectionId)
    );
  }

  buildActionOptions(props) {
    const actions = [
      {
        type: "edit",
        id: props.id,
        onClickEvent: this.onEditHandler
      },
      {
        type: "delete",
        id: props.id,
        onClickEvent: this.onRemoveHandler
      }
    ];
    return <TableRowActions actions={actions} />;
  }

  onRowClick(e, t, rowInfo) {
    console.log(rowInfo);
    this.setState({ highlighted_row: rowInfo.original.id });
  }

  render() {
    const mappedColumns = [...tableColumns].map(column => {
      if (column.key === "tags") {
        return {
          ...column,
          apiCall: this.props.tagsList
        };
      }
      return column;
    });
    mappedColumns.push({
      title: "Actions",
      key: "id",
      width: 130,
      render: props => this.buildActionOptions(props)
    });

    let moreFilters = {};
    if (this.props.moreFilters !== undefined) {
      moreFilters = {
        ...moreFilters,
        ...this.props.moreFilters
      };
    }

    if (this.state.delete_loading) {
      return <Loader />;
    }

    return (
      <ServerPaginatedTable
        uri="sections"
        moreFilters={moreFilters}
        partialFilters={this.state.partial_filters}
        style={{ overflow: "wrap" }}
        getTrProps={(state, rowInfo, column) => {
          return {
            onMouseOver: (e, t) => {
              this.onRowClick(e, t, rowInfo);
            }
          };
        }}
        columns={mappedColumns}
        hasFilters={false}
      />
    );
  }
}

export default ErrorWrapper(connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(QuestionsListPage));
