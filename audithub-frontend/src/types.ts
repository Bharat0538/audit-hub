export type ActionType =
  | 'CREATE' | 'UPDATE' | 'DELETE' | 'READ'
  | 'LOGIN' | 'LOGOUT' | 'EXPORT' | 'APPROVE'
  | 'REJECT' | 'TRANSFER' | 'CUSTOM';

export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Outcome  = 'SUCCESS' | 'FAILURE' | 'PARTIAL';

export interface Actor {
  userId:      string;
  userEmail?:  string;
  userName?:   string;
  ipAddress?:  string;
  userAgent?:  string;
  sessionId?:  string;
}

export interface Action {
  type:         ActionType;
  name:         string;
  description?: string;
}

export interface Resource {
  type:  string;
  id:    string;
  name?: string;
  path?: string;
}

export interface FieldChange {
  fieldName: string;
  oldValue?: unknown;
  newValue?: unknown;
}

export interface AuditEvent {
  eventId:       string;
  organizationId:string;
  applicationId: string;
  eventTime:     string;   // ISO 8601
  actor:         Actor;
  action:        Action;
  resource:      Resource;
  changes:       FieldChange[];
  outcome:       Outcome;
  severity:      Severity;
  correlationId?:string;
  metadata?:     Record<string, string>;
  tags?:         string[];
  createdAt:     string;
}

export interface AuditEventPage {
  content:       AuditEvent[];
  pageToken?:    string | null;
  hasMore:       boolean;
  totalEstimate: number;
}

export interface EventFilters {
  applicationId?: string;
  startTime:      string;
  endTime:        string;
  actorUserId?:   string;
  actorUserEmail?:string;
  resourceType?:  string;
  resourceId?:    string;
  actionTypes?:   ActionType[];
  severities?:    Severity[];
  outcomes?:      Outcome[];
  tags?:          string[];
  query?:         string;
}

export interface DashboardStats {
  totalEventsToday:      number;
  totalEventsThisMonth:  number;
  quotaUsedPercent:      number;
  monthlyQuota:          number;
  criticalEventsToday:   number;
  failedEventsToday:     number;
  activeApplications:    number;
  topActors:             TopActor[];
  topResources:          TopResource[];
  eventsTrend:           TrendPoint[];
}

export interface TopActor {
  userId:     string;
  userName:   string;
  eventCount: number;
}

export interface TopResource {
  resourceType: string;
  eventCount:   number;
}

export interface TrendPoint {
  date:          string;   // "2025-06-13"
  count:         number;
  criticalCount: number;
  failureCount:  number;
}

export interface AlertRule {
  id:                   string;
  organizationId:       string;
  applicationId?:       string;
  name:                 string;
  description?:         string;
  conditionType:        'THRESHOLD' | 'PATTERN' | 'ANOMALY';
  conditionConfig:      AlertConditionConfig;
  severity:             Severity;
  notificationChannels: string[];
  isActive:             boolean;
  createdAt:            string;
}

export interface AlertConditionConfig {
  actionType?:    string;
  resourceType?:  string;
  actorUserId?:   string;
  threshold:      number;
  windowMinutes:  number;
  groupBy?:       string;
}

export interface ReplayRequest {
  filters: Partial<EventFilters>;
  targetTopic: string;
  reason:      string;
}

export type Role = 'OWNER' | 'ADMIN' | 'AUDITOR' | 'VIEWER' | 'DEVELOPER';

export interface User {
  id:            string;
  organizationId:string;
  email:         string;
  name:          string;
  avatarUrl?:    string;
  status:        'ACTIVE' | 'INVITED' | 'SUSPENDED';
  authProvider:  'LOCAL' | 'GOOGLE' | 'SAML';
  mfaEnabled:    boolean;
  lastLoginAt?:  string;
  createdAt:     string;
}

export interface UserWithRole extends User {
  role: Role;
}

export interface AuthTokens {
  accessToken:  string;
  refreshToken: string;
  expiresIn:    number;
  user: {
    id:    string;
    name:  string;
    email: string;
    role:  Role;
    organizationId: string;
  };
}

export interface LoginRequest {
  email:    string;
  password: string;
  mfaCode?: string;
}

export interface SignupRequest {
  name:          string;
  contactEmail:  string;
  password:      string;
  gstNumber?:    string;
}

export interface ApiResponse<T> {
  success: boolean;
  data:    T;
  error?:  ApiError;
  traceId: string;
}

export interface ApiError {
  code:    string;
  message: string;
  details?: Record<string, string[]>;
}

export interface PageResponse<T> {
  content:        T[];
  pageToken?:     string | null;
  hasMore:        boolean;
  totalEstimate:  number;
}

export interface Organization {
  id:                  string;
  name:                string;
  slug:                string;
  displayName:         string;
  plan:                Plan;
  status:              string;
  contactEmail:        string;
  maxEventsPerMonth:   number;
  retentionDays:       number;
  createdAt:           string;
}

export type Plan = 'FREE' | 'STARTER' | 'PROFESSIONAL' | 'ENTERPRISE';

export interface Application {
  id:             string;
  organizationId: string;
  name:           string;
  slug:           string;
  description?:   string;
  environment:    'PRODUCTION' | 'STAGING' | 'DEVELOPMENT';
  status:         string;
  webhookUrl?:    string;
  createdAt:      string;
}

export interface ApiKey {
  id:          string;
  name:        string;
  keyPrefix:   string;
  keyType:     'WRITE' | 'READ' | 'ADMIN';
  isActive:    boolean;
  lastUsedAt?: string;
  expiresAt?:  string;
  createdAt:   string;
}

export interface ApiKeyCreated extends ApiKey {
  plainTextKey: string;
}

export interface GeneratedReport {
  id:            string;
  name:          string;
  status:        'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  format:        'PDF' | 'CSV' | 'XLSX';
  rowCount?:     number;
  fileSizeBytes?:number;
  downloadUrl?:  string;
  errorMessage?: string;
  requestedBy:   string;
  createdAt:     string;
  completedAt?:  string;
}

export interface UsageStats {
  plan:                string;
  currentPeriodStart:  string;
  currentPeriodEnd:    string;
  eventsUsed:          number;
  eventsLimit:         number;
  usagePercent:        number;
  storageUsedBytes:    number;
  retentionDays:       number;
}
