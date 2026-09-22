import Navbar from '../navigation/Navbar.jsx';

export default function Layout({ children }) {
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Navbar />
      <main className="flex-grow">{children}</main>
      <footer className="mt-auto bg-slate-950 text-slate-300 no-print">
        <div className="section-shell py-10 sm:py-14">
          <div className="grid gap-8 md:grid-cols-4">
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-primary to-blue-600 text-lg font-bold text-white shadow-lg shadow-blue-500/20">V</div>
                <div>
                  <div className="text-lg font-bold text-white">Voyara</div>
                  <div className="text-[10px] uppercase tracking-[0.22em] text-slate-400">Travel smarter</div>
                </div>
              </div>
              <p className="text-sm text-slate-400 max-w-xs">
                Premium trip planning, live flight updates, secure payment journeys, and smart travel experiences.
              </p>
            </div>

            <div>
              <h4 className="mb-3 text-sm font-semibold uppercase tracking-[0.16em] text-slate-100">Company</h4>
              <ul className="space-y-2 text-sm text-slate-400">
                <li>About</li>
                <li>Careers</li>
                <li>Press</li>
              </ul>
            </div>

            <div>
              <h4 className="mb-3 text-sm font-semibold uppercase tracking-[0.16em] text-slate-100">Support</h4>
              <ul className="space-y-2 text-sm text-slate-400">
                <li>Help Center</li>
                <li>Cancellation Policy</li>
                <li>Report Issue</li>
              </ul>
            </div>

            <div>
              <h4 className="mb-3 text-sm font-semibold uppercase tracking-[0.16em] text-slate-100">Explore</h4>
              <ul className="space-y-2 text-sm text-slate-400">
                <li>Flights</li>
                <li>Hotels</li>
                <li>Holidays</li>
              </ul>
            </div>
          </div>

          <div className="mt-8 border-t border-slate-800 pt-6 text-center text-xs text-slate-500">
            © 2026 Voyara. Built for real travel booking journeys.
          </div>
        </div>
      </footer>
    </div>
  );
}
