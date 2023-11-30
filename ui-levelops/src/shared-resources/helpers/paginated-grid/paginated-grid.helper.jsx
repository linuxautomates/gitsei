import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import Loader from "components/Loader/Loader";
import { default as AntRow } from "shared-resources/components/ant-row/ant-row.component";
import { AntColComponent as AntCol } from "shared-resources/components/ant-col/ant-col.component";
import "./paginated-grid.style.scss";
import { getError, getLoading } from "../../../utils/loadingUtils";
import { AntPagination } from "../../components";

export class PaginatedGridHelper extends React.Component {
  constructor(props) {
    super(props);
    //this.props.paginationSet(this.props.restCall, null);
    this.state = {
      page: 1,
      page_size: 100,
      tags_loading: false,
      loading: true,
      more_filters: props.moreFilters,
      partial_filters: props.partialFilters,
      refetch_data: false,
      records: [],
      reload: props.reload
    };
    this.fetchData = this.fetchData.bind(this);
    this.onPageChangeHandler = this.onPageChangeHandler.bind(this);
  }

  componentDidMount() {
    console.log("fetching data on mount");
    this.setState(
      {
        loading: true,
        tags_loading: false,
        more_filters: this.props.moreFilters,
        partial_filters: this.props.partialFilters
      },
      () => this.fetchData()
    );
    //this.fetchData();
  }

  static getDerivedStateFromProps(props, state) {
    let filter1 = JSON.parse(JSON.stringify(props.moreFilters));
    let filter2 = JSON.parse(JSON.stringify(state.more_filters));
    const moreFiltersChange = PaginatedGridHelper.compareFilters(filter1, filter2);

    let pfilter1 = JSON.parse(JSON.stringify(props.partialFilters));
    let pfilter2 = JSON.parse(JSON.stringify(state.partial_filters));
    const partialFiltersChange = PaginatedGridHelper.compareFilters(pfilter1, pfilter2);

    if (
      !moreFiltersChange ||
      !partialFiltersChange
      //|| (props.refetchData && props.refetchData !== state.refetch_data)
    ) {
      console.log("filters changed, fetching data");
      let filters = {
        page: state.page - 1,
        page_size: props.pageSize,
        filter: {
          ...props.moreFilters,
          partial: { ...props.partialFilters }
        }
      };
      props.restAction(filters);
      return {
        ...state,
        loading: true,
        tags_loading: false,
        more_filters: props.moreFilters,
        partial_filters: props.partialFilters
      };
    }

    if (state.loading) {
      const { uri, method } = props;
      let tagsLoading = false;
      if (!getLoading(props.rest_api, uri, method, "0") && !getError(props.rest_api, uri, method, "0")) {
        const records = props.rest_api[uri][method]["0"].data.records;
        if (records.length > 0 && records[0].hasOwnProperty("tags")) {
          let bulkTags = [];
          records.forEach(record => {
            const missingTags = record.tags.filter(tag => !bulkTags.includes(tag));
            bulkTags.push(...missingTags);
          });
          if (bulkTags.length > 0) {
            tagsLoading = true;
            console.log("fetching tags after loading");
            props.tagsBulkList({
              filter: {
                tag_ids: bulkTags
              }
            });
          }
        }
        props.restapiLoading(true, uri, method, "0");
        return {
          ...state,
          loading: false,
          tags_loading: tagsLoading,
          records: records
        };
      }
    }

    if (state.tags_loading) {
      if (!getLoading(props.rest_api, "tags", "bulk", "0") && !getError(props.rest_api, "tags", "bulk", "0")) {
        console.log("Updating tags in records");
        const tags = props.rest_api.tags.bulk["0"].data.records;
        const { uri, method } = props;
        props.rest_api[uri][method]["0"].data.records.forEach(record => {
          const tagRec = tags.filter(tag => record.tags.includes(tag.id));
          record.tagObjs = tagRec.map(tag => ({ key: tag.id, label: tag.name }));
        });
        props.restapiClear("tags", "bulk", "0");
        return {
          ...state,
          tags_loading: false,
          loading: false,
          records: props.rest_api[uri][method]["0"].data.records
        };
      }
    }

    if (props.reload !== state.reload) {
      let filters = {
        page: state.page - 1,
        page_size: props.pageSize,
        filter: {
          ...props.moreFilters,
          partial: { ...props.partialFilters }
        }
      };
      props.restAction(filters);
      return {
        ...state,
        reload: props.reload,
        loading: true,
        tags_loading: false
      };
    }

    return null;
  }

  static compareFilters(filter1, filter2) {
    if (filter1 === undefined || filter1 === null || filter2 === undefined || filter2 === null) {
      return true;
    }

    let newFilter1 = JSON.parse(JSON.stringify(filter1));
    let newFilter2 = JSON.parse(JSON.stringify(filter2));

    Object.keys(newFilter1).forEach(key => {
      if (!newFilter2.hasOwnProperty(key)) {
        return false;
      }
      if (Array.isArray(newFilter1[key])) {
        let array1 = newFilter1[key];
        let array2 = newFilter2[key];
        if (array1.length !== array2.length) {
          return false;
        }
        array1.forEach(item => {
          if (array2.filter(i => i === item).length === 0) {
            return false;
          }
        });
      } else {
        if (newFilter1[key] !== newFilter2[key]) {
          return false;
        }
      }

      delete newFilter1[key];
      delete newFilter2[key];
    });
    // console.log('FILTERS!!!!!!', newFilter1, newFilter2);
    return !(Object.keys(newFilter1).length > 0 || Object.keys(newFilter2).length > 0);
  }

  fetchData() {
    let filters = {
      page: this.state.page - 1,
      page_size: this.props.pageSize,
      filter: {
        ...this.props.moreFilters,
        partial: { ...this.props.partialFilters }
      }
    };
    console.log(filters);

    this.setState({ loading: true, tags_loading: false }, () => {
      this.props.restAction(filters);
      //this.props.restapiClear(uri,method,"0");
      //this.props.restAction(filters)
    });
  }

  onPageChangeHandler(page) {
    this.setState(
      {
        page
      },
      () => this.fetchData()
    );
  }

  render() {
    const { className, uri, method, itemsPerRow } = this.props;
    if (this.state.tags_loading || this.state.loading) {
      return <Loader />;
    }
    const restData = this.props.rest_api[uri][method]["0"].data || [];
    //const records = [...restData.records] || [];
    const records = this.state.records;
    const itemSpan = Math.max(6, Math.floor(24 / itemsPerRow));
    return (
      <div className={`${className}`}>
        <div className={`${className} `}>
          <AntRow gutter={[16, 16]} type={"flex"} justify={"start"}>
            {records.map((data, index) => {
              return <AntCol span={itemSpan}>{this.props.mapData(data, index)}</AntCol>;
            })}
          </AntRow>
        </div>
        {this.props.showPagination && (
          <div className={`${className}`}>
            <AntRow type={"flex"} justify={"center"}>
              <AntPagination
                pageSize={this.props.pageSize}
                current={this.state.page}
                onPageChange={this.onPageChangeHandler}
                total={restData._metadata.total_count}
                showPageSizeOptions={false}
                size={"small"}
              />
            </AntRow>
          </div>
        )}
      </div>
    );
  }
}

PaginatedGridHelper.propTypes = {
  className: PropTypes.string,
  restCall: PropTypes.func.isRequired,
  moreFilters: PropTypes.object,
  partialFilters: PropTypes.object,
  mapData: PropTypes.func.isRequired,
  itemsPerRow: PropTypes.number.isRequired,
  pageSize: PropTypes.number.isRequired,
  refetchData: PropTypes.bool,
  restAction: PropTypes.func.isRequired,
  uri: PropTypes.string.isRequired,
  method: PropTypes.string.isRequired,
  showPagination: PropTypes.bool.isRequired,
  reload: PropTypes.number.isRequired
};

PaginatedGridHelper.defaultProps = {
  className: "paginated-grid",
  moreFilters: {},
  partialFilters: {},
  mapData: data => data,
  itemsPerRow: 4,
  pageSize: 12,
  refetchData: false,
  method: "list",
  showPagination: true,
  reload: 1
};

//export default connect(mapPaginationStatetoProps, mapPaginationDispatchtoProps)(PaginatedGridHelper);

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(PaginatedGridHelper);
