import React from 'react'
import {Link} from 'react-router-dom'

const NotificationItem = props => {
  if (!props.notification || !props.notification.user) {
    return ""
  }
  return <Link to={props.notification.url}>
    <p>{"New " + props.notification.type + " from " + props.notification.user.username}</p>
  </Link>
};
export default NotificationItem