import React from "react";
import { Link } from 'react-router-dom'

//To show this in a card's footer, add it to the actions[] prop of the AntCard component
const ViewDetails = ({ linkTo }) => {
  return <Link href={linkTo}>View details</Link>;
};

export default ViewDetails;
