import { Avatar, initials } from './Avatar';
import styles from './Topbar.module.css';

interface TopbarProps {
  title: string;
  subtitle: string;
  userName: string;
  userRole: string;
}

export function Topbar({ title, subtitle, userName, userRole }: TopbarProps) {
  return (
    <div className={styles.bar}>
      <div>
        <div className={styles.title}>{title}</div>
        <div className={styles.subtitle}>{subtitle}</div>
      </div>
      <div className={styles.right}>
        <div className={styles.userChip}>
          <Avatar name={userName} size={34} />
          <span className={styles.userText}>
            <span className={styles.userName}>{userName}</span>
            <span className={styles.userRole}>{userRole}</span>
          </span>
        </div>
      </div>
    </div>
  );
}

export { initials };
