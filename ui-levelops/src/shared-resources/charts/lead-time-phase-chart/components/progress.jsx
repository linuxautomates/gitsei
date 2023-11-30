import React from "react";

export const Progress = props => {
  return (
    <svg {...props} viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" fill="none" height="120" width="120">
      <path
        className="ant-progress-circle-path"
        d="M 50,50 m 0,-47 a 47,47 0 1 1 0,94 a 47,47 0 1 1 0,-94"
        stroke="#d6e4ff"
        strokeLinecap="round"
        strokeDasharray="164 146"
        strokeWidth="6"
        strokeDashoffset="90"
        opacity="1"
        fillOpacity="0"
        fill="none"
      />
    </svg>
  );
};
