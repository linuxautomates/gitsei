import { AntButton, AntCheckbox, AntModal } from "../components";
import { onFilterChange } from "./helper";
import { BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { default as TiltedAxisTick } from "./components/tilted-axis-tick";
import React from "react";

export const GraphModal = (props: any) => (
  <AntModal
    title="Detail view"
    wrapClassName={"bar-chart-modal"}
    visible={props.showModal}
    onCancel={() => props.setShowModal(false)}
    footer={[
      <AntButton key="back" onClick={() => props.setShowModal(false)}>
        Close
      </AntButton>
    ]}>
    <>
      <span>Filters: &nbsp;</span>
      {Object.keys(props.filters).map(key => (
        <AntCheckbox
          checked={props.filters[key]}
          onChange={(value: any) =>
            onFilterChange(key, value.target.checked, props.filters, props.setFilters, props.id)
          }>
          <span style={{ textTransform: "capitalize" }}>{key.replace(/_/g, " ")}</span>
        </AntCheckbox>
      ))}
    </>
    <div style={{ height: "500px" }}>
      <ResponsiveContainer>
        {props.type === "bar" && (
          <BarChart data={props.filteredData} {...props.chartProps} onClick={(data: any) => props.onBarClick(data)}>
            <CartesianGrid horizontal={true} vertical={false} strokeWidth={0.5} />
            <XAxis
              dataKey="name"
              interval={"preserveStartEnd"}
              minTickGap={props.filteredData?.length > 50 ? 10 : props.filteredData?.length > 25 ? 5 : 1}
              tick={<TiltedAxisTick />}
            />
            {
              // @ts-ignore
              <YAxis allowDecimals={false} label={{ value: props.unit, angle: -90, position: "insideLeft" }} />
            }

            <Tooltip cursor={false} content={props.renderTooltip} />
            {props.showLegend && <Legend />}
            {props.bars}
          </BarChart>
        )}
      </ResponsiveContainer>
    </div>
  </AntModal>
);
