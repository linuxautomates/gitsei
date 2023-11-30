import { tableCell } from "utils/tableUtils";

export const tmtTableColumns = [
  {
    title: "Summary",
    key: "summary",
    dataIndex: "summary",
    width: 300,
    ellipsis: true
  },
  {
    title: "Category",
    key: "category",
    dataIndex: "category",
    width: 150
  },
  {
    title: "Severity",
    key: "priority",
    dataIndex: "priority",
    width: 100
  },
  {
    title: "State",
    key: "state",
    dataIndex: "state",
    width: 100,
    ellipsis: true
  }
];

export const prtTableColumns = [
  {
    title: "Summary",
    key: "id",
    dataIndex: "id",
    width: 250,
    ellipsis: true
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    width: 250,
    ellipsis: true
  },
  {
    title: "Category",
    key: "category",
    dataIndex: "meta",
    width: 100,
    ellipsis: true,
    render: (item, record, index) => {
      return item.category;
    }
  }
  // {
  //     title: "Scores",
  //     key: "meta",
  //     dataIndex: "meta",
  //     width: 200,
  //     ellipsis:true,
  //     render: (item,record,index) => {
  //         const scores = Object.keys(item.score).map(
  //             score => {
  //                 return(
  //                     <Col span={4}>
  //                         <Tooltip title={score}>
  //                             <Avatar>{item.score[score].value}</Avatar>
  //                         </Tooltip>
  //                     </Col>
  //                 );
  //             }
  //
  //         );
  //         return (
  //             <Row gutter={[3,3]} justify={"space-between"}>
  //                 {scores}
  //             </Row>
  //         );
  //     }
  // },
];

export const synkColumns = [
  {
    title: "ID",
    key: "id",
    dataIndex: "id",
    width: "30%",
    ellipsis: true
  },
  {
    title: "Title",
    key: "title",
    dataIndex: "title",
    width: "30%",
    ellipsis: true
  },
  {
    title: "CVSS Score",
    key: "cvssScore",
    dataIndex: "cvssScore",
    width: 100,
    ellipsis: true,
    align: "center"
  },
  {
    title: "Detected",
    key: "disclosureTime",
    dataIndex: "disclosureTime",
    width: 100,
    ellipsis: true,
    align: "center",
    render: (item, record, index) => tableCell("created_at", item / 1000)
  }
];

export const tenableColumns = [
  {
    title: "ID",
    key: "id",
    dataIndex: "id",
    width: "30%",
    ellipsis: true
  },
  {
    title: "Asset Name",
    key: "asset_name",
    dataIndex: "asset_name",
    width: "30%",
    ellipsis: true
  },
  {
    title: "Severity",
    key: "vuln_severity",
    dataIndex: "vuln_severity",
    width: "15%",
    ellipsis: true
  },
  {
    title: "Last Found",
    key: "asset_vuln_last_found",
    dataIndex: "asset_vuln_last_found",
    width: 100,
    ellipsis: true,
    align: "center",
    render: (item, record, index) => tableCell("created_at", item)
  }
];

export const nccColumns = [
  {
    title: "Title",
    key: "id",
    dataIndex: "id",
    width: "30%",
    ellipsis: true
  },
  {
    title: "Identifier",
    key: "identifier",
    dataIndex: "identifier",
    width: 150,
    ellipsis: true,
    align: "center"
  },
  {
    title: "Category",
    key: "category",
    dataIndex: "category",
    ellipsis: true,
    align: "center"
  },
  {
    title: "Location",
    key: "location",
    dataIndex: "location",
    ellipsis: true,
    align: "center",
    width: "30%"
  },
  {
    title: "Impact",
    key: "impact",
    dataIndex: "impact",
    ellipsis: true,
    align: "center",
    width: 100
  }
];
