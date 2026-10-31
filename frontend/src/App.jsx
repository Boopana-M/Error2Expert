import React, { useState, useEffect, useCallback } from 'react';
import Navbar from './components/Navbar';
import OverviewCards from './components/OverviewCards';
import MistakeProfile from './components/MistakeProfile';
import WeaknessesList from './components/WeaknessesList';
import TimelineTrends from './components/TimelineTrends';
import Recommendations from './components/Recommendations';
import SubmissionModal from './components/SubmissionModal';
import AuthModal from './components/AuthModal';
import { api } from './services/api';
import { AlertCircle, RefreshCw } from 'lucide-react';
import './App.css';

export default function App() {
  const [currentUser, setCurrentUser] = useState(() => api.getCurrentUser());
  const [isAuthOpen, setIsAuthOpen] = useState(() => !api.isAuthenticated());
  const [summary, setSummary] = useState(null);
  const [mistakeProfile, setMistakeProfile] = useState([]);
  const [weaknesses, setWeaknesses] = useState([]);
  const [timeline, setTimeline] = useState([]);
  const [todaysFocus, setTodaysFocus] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);

  const loadDashboardData = useCallback(async () => {
    if (!api.isAuthenticated()) {
      setSummary(null);
      setMistakeProfile([]);
      setWeaknesses([]);
      setTimeline([]);
      setTodaysFocus(null);
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      const [sumData, profData, weakData, timeData, focusData] = await Promise.all([
        api.getDashboardSummary(),
        api.getMistakeProfile(),
        api.getWeaknesses(),
        api.getTimeline(),
        api.getTodaysFocus(),
      ]);

      setSummary(sumData);
      setMistakeProfile(profData);
      setWeaknesses(weakData);
      setTimeline(timeData);
      setTodaysFocus(focusData);
    } catch (err) {
      if (err.message === 'UNAUTHORIZED') {
        setCurrentUser(null);
        setIsAuthOpen(true);
      } else {
        console.error('Error loading Error2Expert dashboard:', err);
        setErrorMessage('Unable to connect to Error2Expert backend. Please ensure services are running.');
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (api.isAuthenticated()) {
      loadDashboardData();
      const token = localStorage.getItem('codememory_token');
      const user = api.getCurrentUser();
      if (token) {
        window.postMessage({ type: 'ERROR2EXPERT_AUTH_SYNC', token, user }, '*');
        window.dispatchEvent(new CustomEvent('ERROR2EXPERT_AUTH_SYNC', { detail: { token, user } }));
      }
    }
  }, [loadDashboardData]);

  const handleLoginSuccess = (userData) => {
    setCurrentUser(userData);
    setIsAuthOpen(false);
    loadDashboardData();
    const token = localStorage.getItem('codememory_token');
    if (token) {
      window.postMessage({ type: 'ERROR2EXPERT_AUTH_SYNC', token, user: userData }, '*');
      window.dispatchEvent(new CustomEvent('ERROR2EXPERT_AUTH_SYNC', { detail: { token, user: userData } }));
    }
  };

  const handleLogout = () => {
    api.logout();
    setCurrentUser(null);
    setSummary(null);
    setMistakeProfile([]);
    setWeaknesses([]);
    setTimeline([]);
    setTodaysFocus(null);
    setIsAuthOpen(true);
  };

  const handleResetDashboard = async () => {
    if (window.confirm("Are you sure you want to reset your dashboard to a completely fresh, empty state?")) {
      try {
        await api.resetMyDashboard();
        await loadDashboardData();
      } catch (err) {
        alert("Failed to reset dashboard: " + err.message);
      }
    }
  };

  return (
    <div className="app-wrapper">
      <Navbar
        user={currentUser}
        onLogout={handleLogout}
        onResetDashboard={handleResetDashboard}
        onOpenAuthModal={() => setIsAuthOpen(true)}
        onOpenSubmissionModal={() => {
          if (!api.isAuthenticated()) {
            setIsAuthOpen(true);
          } else {
            setIsModalOpen(true);
          }
        }}
      />

      <main className="main-content">
        {errorMessage && (
          <div className="status-banner error" style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <AlertCircle size={16} />
              <span>{errorMessage}</span>
            </div>
            <button
              onClick={loadDashboardData}
              className="btn-secondary btn-sm"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', padding: '4px 10px' }}
            >
              <RefreshCw size={12} className={loading ? 'pulsing-indicator' : ''} />
              <span>Retry</span>
            </button>
          </div>
        )}

        {/* Section 20.1: Longitudinal Overview & Mastery Gauge */}
        <section aria-label="Dashboard Overview">
          <OverviewCards summary={summary} />
        </section>

        {/* 2-Column Section: Mistake Profile & Top Weaknesses */}
        <div className="dashboard-columns">
          <div className="flex flex-col gap-6">
            {/* Section 20.2: Mistake Profile Breakdown */}
            <section aria-label="Mistake Profile">
              <MistakeProfile profile={mistakeProfile} />
            </section>

            {/* Section 20.4: Mistake Evolution Timeline */}
            <section aria-label="Mistake Timeline">
              <TimelineTrends timeline={timeline} />
            </section>
          </div>

          <div>
            {/* Section 20.3: Longitudinal Weaknesses Stack */}
            <section aria-label="Longitudinal Weaknesses">
              <WeaknessesList weaknesses={weaknesses} />
            </section>
          </div>
        </div>

        {/* Section 20.5: Today's Focus & Targeted Curriculum */}
        <section aria-label="Personalized Recommendations">
          <Recommendations focusData={todaysFocus} />
        </section>
      </main>

      {/* Attempt Simulator Modal */}
      <SubmissionModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={loadDashboardData}
      />

      {/* Authentication Modal (Sign In / Register) */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => {
          if (api.isAuthenticated()) {
            setIsAuthOpen(false);
          }
        }}
        onSuccess={handleLoginSuccess}
      />
    </div>
  );
}
