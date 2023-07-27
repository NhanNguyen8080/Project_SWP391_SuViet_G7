import { VerticalTimeline, VerticalTimelineElement } from 'react-vertical-timeline-component';
import { FaStar } from 'react-icons/fa';
import { BsBookmarkStarFill } from 'react-icons/bs';
import 'react-vertical-timeline-component/style.min.css';
import { useEffect, useState } from 'react';
import { Button } from 'react-bootstrap';
import axios from 'axios';
import { useRef } from 'react';

export default function FilterVideoPage({setSelectedPeriod}) {

  const [listPeriods, setListPeriods] =useState([]);
  const timelineRef = useRef(null);


  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await axios.get(`http://localhost:8080/api/period/videos?periodName`);
        setListPeriods(response.data.data)
      } catch (error) {
        console.error('Error fetching data:', error);
      }
    };
    fetchData();
  }, []);

  const handlePeriodClick = (periodName) => {
    setSelectedPeriod(periodName);
    timelineRef.current.scrollIntoView({ behavior: 'smooth' });
  };

  return (<>
    <VerticalTimeline >
      <div ref={timelineRef}>
      {listPeriods.map(period => (
        <VerticalTimelineElement key={period.periodID}
          className="vertical-timeline-element--work"
          contentStyle={{ background: '#FFC701', color: 'black' }}
          contentArrowStyle={{ borderRight: `7px solid black` }}
          // date={<span style={{ color: 'white', fontWeight:'bold', fontSize:'25px'}}>{period.year}</span>}
          iconStyle={{ background: 'black', color: '#FFC701' }}
          icon={<BsBookmarkStarFill />}
        >
          <Button onClick={() => handlePeriodClick(period.periodName)} 
            style={{ fontSize: '15px', fontWeight: 'bold', paddingLeft:'15px', backgroundColor:'#FFC701', border:'none', color:'black'}} 
            className="vertical-timeline-element-title">
            {period.periodName}
          </Button>
        </VerticalTimelineElement>
      ))}
      <VerticalTimelineElement
        iconStyle={{ background: 'rgb(16, 204, 82)', color: 'yellow' }}
        icon={<FaStar />}
      />
      </div>
    </VerticalTimeline>
  </>)
}