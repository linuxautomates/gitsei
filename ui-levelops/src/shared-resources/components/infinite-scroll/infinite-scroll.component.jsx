import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { mapPaginationDispatchtoProps } from "reduxConfigs/maps/paginationMap";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";

import ErrorWrapper from "hoc/errorWrapper";
import "./infinte-scroll.style.scss";
import { debounce, isEqual, uniqBy } from "lodash";
import { genericPaginationData } from "reduxConfigs/selectors/restapiSelector";
import { List, Spin } from "antd";

export class InfiniteScrollComponent extends React.PureComponent {
  constructor(props) {
    super(props);

    this.prevScroll = 0;
    this.scrollRef = 0;

    this.allRecordsRef = React.createRef();

    this.state = {
      page: props.page,
      reload: 1
    };

    this.setScrollPosition = this.setScrollPosition.bind(this);
    this.onScrollHandler = this.onScrollHandler.bind(this);
    this.getClasses = this.getClasses.bind(this);
    this.setScrollRef = this.setScrollRef.bind(this);
    this.onHorizontalScrollHandler = this.onHorizontalScrollHandler.bind(this);
    this.onVerticalScrollHandler = this.onVerticalScrollHandler.bind(this);
    this.loadData = this.loadData.bind(this);
    this.debouncedLoadData = debounce(this.loadData, 500);
    //this.props.paginationSet(this.props.uri, this.props.method, this.props.cancelToken);
  }

  componentDidMount() {
    const { position, loadOnMount } = this.props;

    if (position) {
      this.setScrollPosition(position);
    }

    if (loadOnMount) {
      this.loadData();
    }
  }

  loadData() {
    const filters = {
      page: this.state.page,
      page_size: this.props.pageSize,
      filter: this.props.filters
    };
    const { uri, method, uuid, derive, shouldDerive } = this.props;
    this.props.paginationGet(uri, method, filters, uuid, derive, shouldDerive);
  }

  componentDidUpdate(prevProps, prevState) {
    const { position, reload, page } = this.props;

    if (position !== prevProps.position) {
      this.setScrollPosition(position);
    }

    if (this.state.page !== prevState.page) {
      this.loadData();
    }

    if (this.state.page !== page) {
      this.setState({ page }, () => this.loadData());
    }

    if (reload !== prevProps.reload) {
      this.loadData();
    }

    if (!isEqual(this.props.filters, prevProps.filters)) {
      if (this.props.uuid === prevProps.uuid) {
        // if uuid changed then component mount should take care of it
        this.debouncedLoadData();
      }
    }
  }

  componentWillMount() {
    //this.props.paginationClear();
    this.props.restapiClear(this.props.uri, this.props.method, "0");
  }

  onScrollHandler() {
    let scrolledTo = 0;

    if (this.props.horizontal) {
      this.onHorizontalScrollHandler();
      scrolledTo = this.scrollRef.scrollLeft;
    } else {
      this.onVerticalScrollHandler();
      scrolledTo = this.scrollRef.scrollTop;
    }

    this.props.onScrollEvent(scrolledTo, this.prevScroll);
    this.prevScroll = scrolledTo;
  }

  onHorizontalScrollHandler() {
    if (this.scrollRef) {
      const { firstChild, lastChild, scrollLeft, offsetLeft, offsetWidth } = this.scrollRef;
      const leftEdge = firstChild ? firstChild.offsetLeft : 0;
      const rightEdge = lastChild ? lastChild.offsetLeft + lastChild.offsetWidth : 0;
      const scrolledLeft = scrollLeft + offsetLeft;
      const scrolledRight = scrolledLeft + offsetWidth;
      if (scrolledRight >= rightEdge && !this.props.pagination_loading) {
        this.setState(state => ({
          page: state.page + 1
        }));
        this.props.onReachRight();
      } else if (scrolledLeft <= leftEdge && this.state.page > 1 && !this.props.pagination_loading) {
        this.setState(state => ({
          page: state.page - 1
        }));
        this.props.onReachLeft();
      }
    }
  }

  onVerticalScrollHandler() {
    const { firstChild, lastChild, scrollTop, offsetTop, offsetHeight } = this.scrollRef;

    const topEdge = firstChild ? firstChild.offsetTop : 0;
    const bottomEdge = lastChild ? lastChild.offsetTop + lastChild.offsetHeight : 0;
    const scrolledUp = scrollTop + offsetTop;
    const scrolledDown = scrolledUp + offsetHeight;

    if (scrolledDown >= bottomEdge) {
      this.props.onReachBottom();
    } else if (scrolledUp <= topEdge) {
      this.props.onReachTop();
    }
  }

  setScrollPosition(position = 0) {
    if (this.props.horizontal) {
      this.scrollRef.scrollLeft = position;
    } else {
      this.scrollRef.scrollTop = position;
    }

    this.prevScroll = position;
  }

  getClasses() {
    const { className } = this.props;
    const classes = [className];
    if (this.props.horizontal) {
      classes.push(`${className}__horizontal`);
    }
    return classes.join(" ");
  }

  setScrollRef(ref) {
    this.scrollRef = ref;
  }

  get records() {
    const { paginationState, dataFilter } = this.props;
    const data = paginationState.data || {};
    const records = data.records || [];
    const newRecords = records.filter(row => !dataFilter.values.includes(row[dataFilter.field]));
    const allNewRecords = uniqBy([...(this.allRecordsRef.current ?? []), ...newRecords], "id");
    this.allRecordsRef.current = allNewRecords;
    return allNewRecords;
  }

  render() {
    const { paginationState, renderItem } = this.props;
    const loading = paginationState.loading !== undefined ? paginationState.loading : true;

    if (loading) {
      return (
        <div align={"center"}>
          <Spin />
        </div>
      );
    }

    return (
      <div ref={this.setScrollRef} className={this.getClasses()} onScroll={this.onScrollHandler} role="presentation">
        <List dataSource={this.records} loading={loading} renderItem={renderItem} />
      </div>
    );
  }
}

const stateToProps = (state, ownProps) => {
  return {
    paginationState: genericPaginationData(state, ownProps)
  };
};

const dispatchToProps = dispatch => {
  return {
    ...mapRestapiDispatchtoProps(dispatch),
    ...mapPaginationDispatchtoProps(dispatch)
  };
};

InfiniteScrollComponent.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
  horizontal: PropTypes.bool,
  onReachBottom: PropTypes.func,
  onReachTop: PropTypes.func,
  onReachLeft: PropTypes.func,
  onReachRight: PropTypes.func,
  onScrollEvent: PropTypes.func,
  position: PropTypes.number,
  restMethod: PropTypes.string.isRequired,
  page: PropTypes.number,
  pageSize: PropTypes.number,
  uri: PropTypes.string,
  method: PropTypes.string,
  filters: PropTypes.string,
  reload: PropTypes.number,
  uuid: PropTypes.any,
  renderItem: PropTypes.func.isRequired,
  derive: PropTypes.bool,
  loadOnMount: PropTypes.bool
};

InfiniteScrollComponent.defaultProps = {
  className: "infinite-scroll",
  horizontal: false,
  onReachBottom: () => null,
  onReachTop: () => null,
  onReachLeft: () => null,
  onReachRight: () => null,
  onScrollEvent: () => null,
  position: 0,
  page: 0,
  pageSize: 20,
  method: "list",
  filters: {},
  reload: 1,
  uuid: "0",
  selectField: "name",
  derive: true,
  shouldDerive: "all",
  dataFilter: { field: "id", values: [] },
  loadOnMount: true
};

export default ErrorWrapper(connect(stateToProps, dispatchToProps)(InfiniteScrollComponent));
