import React, {Fragment} from 'react'
import {connect} from 'react-redux'
import {loadSearchPosts} from '../actions/searchPostsActions'
import {loadSearchUsers} from '../actions/searchUsersActions'
import PostsContainer from '../containers/PostsContainer'
import UsersContainer from '../containers/UsersContainer'
import EmptyState from '../containers/EmptyState'
import {loadCurrentUser} from '../actions/currentUserActions'
import searchIcon from '../img/search-icon.svg'
import {deletePost} from '../actions/postsActions'
import {deleteComment} from '../actions/commentsActions'
import Loader from "../containers/Loader";

class SearchPage extends React.Component {

  constructor(props) {
    super(props)
    this.state = {
      match: this.props.match.params.input
    }
  }

  componentWillMount() {
    if (this.props.foundPosts.length === 0 && this.props.foundUsers.length === 0) {
      this.props.loadPosts(this.props.match.params.input)
      this.props.loadUsers(this.props.match.params.input)
    }
    if (!this.props.currentUser || !this.props.currentUser.username) {
      this.props.loadCurrentUser()
    }
  }

  componentDidUpdate() {
    this.callUpdate();
    this.searchTimeOut();
  }

  componentDidMount() {
    this.callUpdate();
  }

  searchTimeOut = () => {
    let empty = document.querySelector('.empty-state');
    let loader = document.querySelector('.loader');

    if (this.props.foundPosts.length === 0 && empty !== null && loader !== null) {
      loader.style.display = "flex";
      empty.style.display = "none";

      setTimeout(() => {
        if (this.props.foundPosts.length === 0) {
          loader.style.display = "none";
          empty.style.display = "flex";
        }
      }, 2000)
    }
  };

  callUpdate = () => {
    if (this.props.match.params.input != this.state.match) {
      this.props.loadPosts(this.props.match.params.input)
      this.props.loadUsers(this.props.match.params.input)
      this.setState({match: this.props.match.params.input})
    }
  };

  follow = followedUser => fetch('/api/follow/add',
      {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + localStorage.getItem('accessToken'),
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          follower: {id: this.props.currentUser.id},
          followedUser: {id: followedUser}
        })
      }).then(() => this.props.loadCurrentUser());

  unfollow = followedUser => fetch('/api/follow/delete',
      {
        method: 'DELETE',
        headers: {
          'Authorization': 'Bearer ' + localStorage.getItem('accessToken'),
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          follower: {id: this.props.currentUser.id},
          followedUser: {id: followedUser}
        })
      }).then(() => this.props.loadCurrentUser());

  ShowUsers = () => {
    this.refs.searchUsers.classList.remove("d-none");
    this.refs.searchPosts.classList.add("d-none");
  };

  showPosts = () => {
    this.refs.searchPosts.classList.remove('d-none')
    this.refs.searchUsers.classList.add('d-none')
  };


  render() {
    if (!this.props.currentUser || !this.props.currentUser.username) {
      return <Loader/>
    }
    return (
        <Fragment key={SearchPage.id}>
          <div className="header_search">
            <div onClick={() => this.ShowUsers()} className="btn-toSearch">Users</div>
            <div onClick={() => this.showPosts()} className="btn-toSearch">Posts</div>
          </div>
          <div className="container">
            <div ref="searchPosts" className="post-panel">
              <EmptyState image={searchIcon}
                          title="Oops! Nothing has been found"
                          message={'Sorry, no results found matching your parameters'}/>
              {this.props.foundPosts.length === 0
                  ? <Loader/>
                  : <PostsContainer username={this.props.match.params.input}
                                    userPosts={this.props.foundPosts}
                                    loadData={this.props.loadPosts}
                                    currentUser={this.props.currentUser}
                                    deletePost={this.props.deletePost}
                                    deleteComment={this.props.deleteComment}/>
              }
            </div>
            <div ref="searchUsers" className="users-panel">
              {this.props.foundUsers.length === 0 ? ''
                  : <UsersContainer username={this.props.match.params.input}
                                    loadUsers={this.props.loadUsers}
                                    users={this.props.foundUsers}
                                    currentUser={this.props.currentUser}
                                    follow={this.follow}
                                    unfollow={this.unfollow}
                  />}
            </div>
          </div>
        </Fragment>
    )
  }
}

const mapStateToProps = state => ({
  foundPosts: state.foundPosts,
  foundUsers: state.foundUsers,
  currentUser: state.currentUser
})

const mapDispatchToProps = dispatch => ({
  loadPosts: input => dispatch(loadSearchPosts(input)),
  loadUsers: input => dispatch(loadSearchUsers(input)),
  loadCurrentUser: () => dispatch(loadCurrentUser()),
  deletePost: data => dispatch(deletePost(data)),
  deleteComment: data => dispatch(deleteComment(data))
})

export default connect(mapStateToProps, mapDispatchToProps)(SearchPage)