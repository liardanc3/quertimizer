import AdminPage from '../pages/AdminPage';
import CommunityDetailPage from '../pages/CommunityDetailPage';
import CommunityPage from '../pages/CommunityPage';
import CommunityWritePage from '../pages/CommunityWritePage';
import DashboardPage from '../pages/DashboardPage';
import FavoritePage from '../pages/FavoritePage';
import GuidePage from '../pages/GuidePage';
import HomePage from '../pages/HomePage';
import ProblemSolvePage from '../pages/ProblemSolvePage';
import ProfileActivityPage from '../pages/ProfileActivityPage';
import ProfilePage from '../pages/ProfilePage';
import RankingPage from '../pages/RankingPage';
import SubmitHistoryPage from '../pages/SubmitHistoryPage';
import type { AppRoute } from './routeConfig';

export function renderRouteComponent(route: AppRoute) {
  switch (route.type) {
    case 'problem':
      return <ProblemSolvePage key={route.problemId} problemId={route.problemId} />;
    case 'dashboard':
    case 'home':
      return <DashboardPage />;
    case 'ranking':
      return <RankingPage />;
    case 'submitHistory':
      return <SubmitHistoryPage />;
    case 'favorites':
      return <FavoritePage />;
    case 'community':
      return <CommunityPage />;
    case 'guide':
      return <GuidePage />;
    case 'admin':
      return <AdminPage />;
    case 'communityWrite':
      return <CommunityWritePage key="write" />;
    case 'communityEdit':
      return <CommunityWritePage key={`edit-${route.postId}`} postId={route.postId} />;
    case 'communityDetail':
      return <CommunityDetailPage key={route.postId} postId={route.postId} />;
    case 'profileActivity':
      return <ProfileActivityPage key={route.handle ?? 'me'} handle={route.handle} />;
    case 'profile':
      return <ProfilePage key={route.handle ?? 'me'} handle={route.handle} />;
    case 'problems':
      return <HomePage />;
    default:
      return <DashboardPage />;
  }
}
