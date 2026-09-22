import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout.jsx';
import ScrollToTop from './components/common/ScrollToTop.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';

const Home = lazy(() => import('./pages/Home.jsx'));
const Login = lazy(() => import('./pages/Login.jsx'));
const Register = lazy(() => import('./pages/Register.jsx'));
const VerifyEmail = lazy(() => import('./pages/VerifyEmail.jsx'));
const Dashboard = lazy(() => import('./pages/Dashboard.jsx'));
const TripDetail = lazy(() => import('./pages/TripDetail.jsx'));
const FlightResults = lazy(() => import('./pages/FlightResults.jsx'));
const FlightDetail = lazy(() => import('./pages/FlightDetail.jsx'));
const HotelsHomepage = lazy(() => import('./pages/HotelsHomepage.jsx'));
const HotelResults = lazy(() => import('./pages/HotelResults.jsx'));
const HotelDetail = lazy(() => import('./pages/HotelDetail.jsx'));
const Booking = lazy(() => import('./pages/Booking.jsx'));
const LiveFlightDashboard = lazy(() => import('./pages/LiveFlightDashboard.jsx'));
const HolidaysHomepage = lazy(() => import('./pages/HolidaysHomepage.jsx'));
const HolidaysResults = lazy(() => import('./pages/HolidaysResults.jsx'));
const HolidayDetail = lazy(() => import('./pages/HolidayDetail.jsx'));
const TrainsPage = lazy(() => import('./pages/TrainsPage.jsx'));
const BusesPage = lazy(() => import('./pages/BusesPage.jsx'));
const CabsPage = lazy(() => import('./pages/CabsPage.jsx'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword.jsx'));
const ResetPassword = lazy(() => import('./pages/ResetPassword.jsx'));
const Profile = lazy(() => import('./pages/Profile.jsx'));
const AdminPanel = lazy(() => import('./pages/AdminPanel.jsx'));
const VoyaraRewards = lazy(() => import('./pages/VoyaraRewards.jsx'));
const Notifications = lazy(() => import('./pages/Notifications.jsx'));
const TripOverview = lazy(() => import('./pages/TripOverview.jsx'));
const GroupTrips = lazy(() => import('./pages/GroupTrips.jsx'));
const GroupExpenses = lazy(() => import('./pages/GroupExpenses.jsx'));
const GroupInvitationPage = lazy(() => import('./pages/GroupInvitationPage.jsx'));

function PageLoader() {
  return (
    <div className="section-shell py-14">
      <div className="max-w-4xl mx-auto space-y-5">
        <div className="skeleton h-8 w-64 rounded-xl" />
        <div className="skeleton h-4 w-96 rounded-lg" />
        <div className="skeleton h-56 w-full rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => <div key={i} className="skeleton h-36 rounded-2xl" />)}
        </div>
      </div>
    </div>
  );
}

function NotFound() {
  return (
    <div className="section-shell py-24 text-center">
      <div className="inline-flex h-20 w-20 items-center justify-center rounded-3xl bg-blue-50 text-primary mb-6 shadow-md shadow-blue-500/10">
        <svg className="w-10 h-10" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.75">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
      </div>
      <h1 className="text-3xl font-black text-slate-900 mb-2">Destination not found</h1>
      <p className="text-slate-500 mb-8 max-w-md mx-auto text-sm leading-relaxed">
        The page you are looking for has departed, changed gates, or does not exist.
      </p>
      <a href="/" className="travel-button-primary px-7 py-3 text-sm">
        Return to Home
      </a>
    </div>
  );
}


export default function App() {
  return (
    <Layout>
      <ScrollToTop />
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/my-trips" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
          <Route path="/dashboard/trip/:reference" element={<ProtectedRoute><TripOverview /></ProtectedRoute>} />
          <Route path="/live-tracker" element={<ProtectedRoute><LiveFlightDashboard /></ProtectedRoute>} />
          <Route path="/flights/search" element={<FlightResults />} />
          <Route path="/flights/:id" element={<FlightDetail />} />
          <Route path="/hotels" element={<HotelsHomepage />} />
          <Route path="/hotels/search" element={<HotelResults />} />
          <Route path="/hotels/:id" element={<HotelDetail />} />
          <Route path="/booking" element={<ProtectedRoute><Booking /></ProtectedRoute>} />
          <Route path="/holidays" element={<HolidaysHomepage />} />
          <Route path="/holidays/search" element={<HolidaysResults />} />
          <Route path="/holidays/:id" element={<HolidayDetail />} />
          <Route path="/trains" element={<TrainsPage />} />
          <Route path="/buses" element={<BusesPage />} />
          <Route path="/cabs" element={<CabsPage />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/profile" element={<ProtectedRoute><Profile /></ProtectedRoute>} />
          <Route path="/rewards" element={<ProtectedRoute><VoyaraRewards /></ProtectedRoute>} />
          <Route path="/notifications" element={<ProtectedRoute><Notifications /></ProtectedRoute>} />
          <Route path="/trip/:reference" element={<ProtectedRoute><TripOverview /></ProtectedRoute>} />
          <Route path="/group-trips" element={<ProtectedRoute><GroupTrips /></ProtectedRoute>} />
          <Route path="/group-trips/:id" element={<ProtectedRoute><GroupExpenses /></ProtectedRoute>} />
          <Route path="/group-invitations/:token" element={<GroupInvitationPage />} />
          <Route path="/admin" element={<ProtectedRoute requiredRole="ADMIN"><AdminPanel /></ProtectedRoute>} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Suspense>
    </Layout>
  );
}
