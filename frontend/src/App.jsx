import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import OverviewCards from './components/OverviewCards';
import MistakeProfile from './components/MistakeProfile';
import WeaknessesList from './components/WeaknessesList';
import TimelineTrends from './components/TimelineTrends';
import Recommendations from './components/Recommendations';
import SubmissionModal from './components/SubmissionModal';
import { api } from './services/api';
import './App.css';

export default function App() {
  const [summary, setSummary] = useState(null);
  const [mistakeProfile, setMistakeProfile] = useState([]);
  const [weaknesses, setWeaknesses] = useState([]);
  const [timeline, setTimeline] = useState([]);
  const [todaysFocus, setTodaysFocus] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);

  const loadDashboardData = async () => {
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
      console.error('Error loading CodeMemory dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  return (
    <div className="app-wrapper">
      <Navbar onOpenSubmissionModal={() => setIsModalOpen(true)} />

      <main className="main-content">
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
    </div>
  );
}
