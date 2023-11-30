import React from "react";

export const getDots = (x: number, y: number, width: number, height: number) => {
  const radius = 2;

  const dots: any[] = [];
  const topPadding = 3;
  const verticalGap = 6;
  let currentX = x;
  let currentY = y + radius + topPadding;
  const numberOfDots = 5;

  let row = 0;
  for (let i = currentY; i < y + height; i++) {
    const dotSpace = 2 * radius * (row % 2 === 0 ? numberOfDots + 1 : numberOfDots);
    const freeSpace = width - dotSpace;
    const dx = freeSpace / numberOfDots;

    currentX = row % 2 === 0 ? x + radius : x + dx / 2 + radius;

    for (let j = 0; j < (row % 2 === 0 ? numberOfDots + 1 : numberOfDots); j++) {
      const point = {
        cx: currentX,
        cy: i,
        r: radius,
        fill: "#525252",
        opacity: 0.7
      };

      dots.push(point);

      currentX = currentX + dx + 2 * radius;
    }

    i = i + verticalGap;
    row = row + 1;
  }

  return dots;
};

interface CustomBarProps {
  data: any[];
  index: number;
  x: number;
  y: number;
  width: number;
  height: number;
  fill: string;
}

export const customBar = (props: CustomBarProps) => {
  const { fill, x, y, width, height, index, data } = props;
  return <rect x={x} y={y} width={width} height={height} fill={fill} />;
};
