import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import cx from "classnames";
import { Col, Icon, Row } from "antd";
import { ScoreChartProps } from "../chart-types";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Label,
  Legend,
  LegendProps,
  RadialBar,
  RadialBarChart,
  ResponsiveContainer,
  Tooltip,
  TooltipProps,
  XAxis,
  YAxis
} from "recharts";
import "./score-chart.component.scss";
import { toTitleCase, valueToTitle } from "utils/stringUtils";
import { capitalize, get, orderBy, uniq } from "lodash";
import { AntText, EmptyWidget } from "../../components";
import { scoreColor, scoreChartColor } from "./helper";
import { chartTransparentStaticColors, lineChartColors } from "../chart-themes";
import { renderTooltip } from "../bar-chart/bar-chart.tooltip";
import { ChartType } from "../../containers/chart-container/ChartType";
import BreakDown from "./breakdown/BreakDown";
import ColorScheme from "./color-scheme/ColorScheme";
import LegendComponent from "../components/generic-legend/legend.component";
import { round } from "utils/mathUtils";
import TiltedAxisTick from "../components/tilted-axis-tick";

const ScoreChartComponent = (props: ScoreChartProps) => {
  const { score, data, hasClickEvents, onClick, previewOnly, hideScore, width, hideLegend } = props;

  const breakdown = props.breakdown || data || [];

  const [activeIndex, setActiveIndex] = useState(0);
  const [showTable, setShowTable] = useState(true);
  const [filters, setFilters] = useState<any>({});
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("desc");
  const defaultBarSize: number = 30;

  const type = breakdown.length > 0 && breakdown?.[0].hasOwnProperty("stack_data") ? "jira" : "other";

  useEffect(() => {
    if (breakdown.length) {
      const initialFilters = breakdown.reduce((acc: any, item: any, index: number) => {
        if (index < 8 && item.weight > 0) {
          return { ...acc, [item.hygiene]: true };
        }
        return { ...acc, [item.hygiene]: false };
      }, {});
      setFilters(initialFilters);
    }
  }, [breakdown]);

  let sumScore = 0;
  let maxScore = 0;

  const _validData = useMemo(
    () => orderBy(breakdown, ["score_percent"], ["asc"]).filter((hygiene: any) => hygiene.weight > 0) || [],
    [breakdown]
  ) as any[];

  for (const _validDatum of _validData) {
    const _score = _validDatum.score;
    sumScore += _score;
    if (_score > maxScore) {
      maxScore = _score;
    }
  }

  const dataToShow = useMemo(
    () =>
      _validData
        .filter((item: any) => {
          // in edit-mode filters is not set
          if (Object.keys(filters).length === 0) {
            return true;
          }
          return !!get(filters, [item.hygiene], undefined);
        })
        .map(_validDatum => ({ ..._validDatum, fill: scoreColor(_validDatum["score_percent"]) }))
        .sort((a, b) => (a.score_percent || 0) - (b.score_percent || 0)),
    [_validData, filters]
  );

  const sortedBreakDown = useMemo(() => {
    return (orderBy(dataToShow, ["score_percent"], sortOrder) as any[]) || [];
  }, [dataToShow, sortOrder]);

  useEffect(() => {
    setActiveIndex(dataToShow.length - 1);
  }, []);

  const colorScheme = useMemo(
    () => [
      {
        range: "poor",
        color: scoreChartColor[0]
      },
      {
        range: "improving",
        color: scoreChartColor[1]
      },
      {
        range: "moderate",
        color: scoreChartColor[2]
      },
      {
        range: "good",
        color: scoreChartColor[3]
      }
    ],
    [scoreChartColor]
  );

  const stackData = useMemo(
    () =>
      dataToShow.map((i: any) => ({
        name: i?.id ? valueToTitle(i?.hygiene || "") : toTitleCase(i?.hygiene || ""),
        id: i?.id,
        hygiene: i?.hygiene,
        ...(i?.stack_data || {})
      })),
    [dataToShow]
  );

  const barPropsKeys = useMemo(
    () =>
      dataToShow.map((i: any) => ({ ...i.stack_data })).reduce((acc: any, i: any) => [...acc, ...Object.keys(i)], []),
    [dataToShow]
  );

  const barProps = useMemo(() => [...uniq(barPropsKeys).map((k: any) => ({ name: k, dataKey: k }))], [barPropsKeys]);

  const total = useMemo(
    () =>
      (dataToShow || []).reduce((acc: any, item: any) => {
        return item.all_tickets;
      }, 0),
    [dataToShow]
  );

  const _handleBarChartClick = (item: any) => {
    const { hygiene, id } = item;
    if (id === undefined) {
      hasClickEvents && onClick && onClick(hygiene);
    } else {
      hasClickEvents && onClick && onClick({ hygiene, id });
    }
  };

  const _showCompleteReport = () => {
    hasClickEvents && onClick && onClick("show_all");
  };

  const handleClick = (hygiene: string, id: any) => {
    if (id === undefined) {
      hasClickEvents && onClick && onClick(hygiene);
    } else {
      hasClickEvents && onClick && onClick({ hygiene, id });
    }
  };

  const getTooltipContent = useCallback(
    (tooltipProps: TooltipProps) => {
      return renderTooltip(tooltipProps, { ...props, stacked: true }, ChartType.BAR);
    },
    [props]
  );

  const toggleTable = useCallback(() => {
    setShowTable(!showTable);
  }, [showTable]);

  const toggleSortOrder = useCallback(() => {
    if (sortOrder === "asc") {
      setSortOrder("desc");
    } else {
      setSortOrder("asc");
    }
  }, [sortOrder]);

  const radialBarChartData = useMemo(
    () => [...dataToShow, { score: 100, score_percent: 100, hygiene: " ", fill: "#ffffff" }],
    [dataToShow]
  );

  const totalTickets = useMemo(
    () =>
      (breakdown || []).reduce((acc: any, item: any) => {
        return acc + item?.total_tickets || 0;
      }, 0),
    [breakdown]
  );

  const scoreRowGutter = useMemo(() => [50, 10] as any, []);

  const yAxisLabel = useMemo(() => ({ value: "Count", angle: -90, position: "insideLeft" }), []);

  const legendWrapperStyle = useMemo(
    () => ({
      top: previewOnly ? "-25%" : "4%",
      left: previewOnly ? "42%" : "51%",
      width: "auto",
      transform: previewOnly ? "scale(0.4)" : ""
    }),
    []
  );

  const getLegendContent = useCallback((props: LegendProps) => {
    let size = 12;
    if (props.payload && props.payload.length > 11) {
      size = 9;
    } else if (props.payload && props.payload.length > 9) {
      size = 10;
    } else if (props.payload && props.payload.length > 7) {
      size = 11;
    }

    return (
      <div className="legend-container">
        {props.payload &&
          (props.payload as any[]).reverse().map((item: any, index) => (
            <AntText key={index} style={{ fontSize: `${size}px`, lineHeight: `${size}px`, height: `${size}px` }}>
              {item?.payload?.id ? valueToTitle(item.payload.hygiene) : toTitleCase(item.payload.hygiene)}
            </AntText>
          ))}
      </div>
    );
  }, []);

  const getRadialBarLabel = useCallback(
    props => {
      // https://github.com/levelops/ui-levelops/pull/898/files#diff-4ea6012946c51ae6532244d40efc37e5a7cf4c818907e8a7836597803949ce0bR368 Discuss and remove this check.
      return "";
      if (props.index === dataToShow.length || !props.value) {
        return "";
      }
      return (
        <Label
          position="insideStart"
          fill="#222"
          viewBox={props.viewBox}
          value={props.value}
          offset={props.offset}
          className="score-label"
        />
      );
    },
    [dataToShow]
  );

  const renderRadialBarChart = useMemo(() => {
    return (
      <RadialBarChart
        width={730}
        height={250}
        innerRadius="10%"
        outerRadius="100%"
        data={radialBarChartData}
        startAngle={360}
        endAngle={90}>
        <RadialBar
          label={getRadialBarLabel}
          background
          cornerRadius={10}
          isAnimationActive={false}
          dataKey="score_percent"
        />
        <Legend wrapperStyle={legendWrapperStyle} align="right" verticalAlign="middle" content={getLegendContent} />
      </RadialBarChart>
    );
  }, [dataToShow, radialBarChartData, getRadialBarLabel, legendWrapperStyle, getLegendContent]);

  const tickFormatter = useCallback(
    (value: any) => (typeof value === "number" && value / 1000 >= 1 ? `${round(value / 1000, 2)}k` : value),
    []
  );

  const handleBarChartClick = (data: any) =>
    handleClick(
      data?.activePayload?.[0]?.payload?.hygiene ? data.activePayload[0].payload.hygiene : data?.activeLabel,
      data?.activePayload[0]?.payload?.id
    );

  const renderBars = useMemo(() => {
    return (barProps || []).map((bar: any, i: number) => {
      bar.fill = lineChartColors[i % lineChartColors.length];
      return <Bar cursor={"pointer"} stackId={1} barSize={defaultBarSize} key={`bar-${i}`} {...bar} />;
    });
  }, [barProps, defaultBarSize]);

  const modifyLayoutStyle = useMemo(
    () => ({
      transform: previewOnly ? "scale(0.5)" : ""
    }),
    [props]
  );

  const hygieneListStyle = useMemo(
    () => ({
      maxHeight: previewOnly ? "110px" : ""
    }),
    [props]
  );

  const modifyLayoutFontStyle = useMemo(
    () => ({
      fontSize: previewOnly ? "8px" : ""
    }),
    [props]
  );

  return (
    <div className="score-chart-component-container">
      <Row className="score-row" gutter={scoreRowGutter} align={"middle"}>
        {!hideScore && (
          <Col className="score-col" span={6}>
            <div className="score-container">
              <div className="score" style={modifyLayoutStyle}>
                <AntText className="scoreLabel">HYGIENE SCORE</AntText>
                <span className="score-wrapper">
                  <span className="score-sum">{sumScore}</span>
                  <span className="score-total">/100</span>
                </span>
              </div>
              <div className="vertical-line" />
            </div>
          </Col>
        )}
        <Col
          className={cx({
            "stats-col": type === "jira" || !showTable,
            "breakdown-container": !(type === "jira" || !showTable),
            "align-left-12": hideScore,
            "align-left-23": !hideScore && (type === "jira" || !showTable)
          })}
          span={8}>
          <>
            {showTable && (
              <>
                <div className="stats-header">
                  <div className="stats-title" style={modifyLayoutFontStyle}>
                    Breakdown by contributing factors
                  </div>
                  {type === "jira" && (
                    <div className="chart-btn" onClick={toggleTable}>
                      <Icon type={"bar-chart"} />
                    </div>
                  )}
                </div>
                <div className="ticket-count-container">
                  <p className="ticket-count" onClick={_showCompleteReport} style={modifyLayoutFontStyle}>
                    Total Tickets: <span className="clickable-data-point">{total}</span>
                  </p>
                  <div className="sort-btn" onClick={toggleSortOrder}>
                    <span className="sort-label" style={modifyLayoutFontStyle}>
                      # Issues
                      <Icon type={sortOrder === "asc" ? "caret-down" : "caret-up"} className="sort-icon" />
                    </span>
                  </div>
                </div>
                <div className="hygieneList" style={hygieneListStyle}>
                  <div className="issueList" style={modifyLayoutFontStyle}>
                    {sortedBreakDown.map((item, index) => (
                      <BreakDown
                        key={`breakdown-${index}`}
                        breakdown={item}
                        onClick={_handleBarChartClick}
                        total={total}
                      />
                    ))}
                  </div>
                </div>
              </>
            )}
            {type === "jira" && !showTable && (
              <>
                <div className="list-btn" onClick={toggleTable}>
                  <Icon type={"unordered-list"} />
                </div>
                {totalTickets === 0 && <EmptyWidget />}
                {totalTickets !== 0 && (
                  <ResponsiveContainer>
                    <BarChart data={stackData} {...props.chartProps} onClick={handleBarChartClick}>
                      <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
                      <XAxis
                        hide={previewOnly}
                        dataKey="name"
                        stroke={chartTransparentStaticColors.axisColor}
                        interval={0}
                        minTickGap={3}
                        tick={<TiltedAxisTick />}
                      />
                      {
                        <YAxis
                          hide={previewOnly}
                          stroke={chartTransparentStaticColors.axisColor}
                          allowDecimals={false}
                          // @ts-ignore
                          label={yAxisLabel}
                          tickFormatter={tickFormatter}
                        />
                      }
                      {!props.config?.disable_tooltip && stackData.length > 0 && (
                        <Tooltip cursor={false} content={getTooltipContent} />
                      )}
                      {renderBars}
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </>
            )}
          </>
        </Col>
        <Col span={10} className={hideScore ? "chart-col align-right-10" : "chart-col align-right-0"}>
          <div className="colors-container" style={modifyLayoutStyle}>
            {colorScheme.map((item: any, index) => (
              <ColorScheme key={`color-scheme-${index}`} color={item.color} range={capitalize(item?.range || "")} />
            ))}
          </div>
          <ResponsiveContainer>{renderRadialBarChart}</ResponsiveContainer>
        </Col>
      </Row>
      {!hideLegend && <LegendComponent filters={filters} width={width - 32} setFilters={setFilters} />}
    </div>
  );
};

export default ScoreChartComponent;
