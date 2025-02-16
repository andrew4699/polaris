#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
import os
from enum import Enum


class StorageType(Enum):
    """
    Represents a Storage Type within the Polaris API -- `s3`, `azure`, `gcs`, or `file`.
    """

    S3 = 's3'
    AZURE = 'azure'
    GCS = 'gcs'
    FILE = 'file'


class CatalogType(Enum):
    """
    Represents a Catalog Type within the Polaris API -- `internal` or `external`
    """

    INTERNAL = 'internal'
    EXTERNAL = 'external'


class PrincipalType(Enum):
    """
    Represents a Principal Type within the Polaris API -- currently only `service`
    """

    SERVICE = 'service'


class Commands:
    """
    Represents the various commands available in the CLI
    """

    CATALOGS = 'catalogs'
    PRINCIPALS = 'principals'
    PRINCIPAL_ROLES = 'principal-roles'
    CATALOG_ROLES = 'catalog-roles'
    PRIVILEGES = 'privileges'
    NAMESPACES = 'namespaces'
    PROFILES = 'profiles'


class Subcommands:
    """
    Represents the various subcommands available in the CLI. This is a flattened view, and no one command supports
    all these subcommands.
    """

    CREATE = 'create'
    DELETE = 'delete'
    GET = 'get'
    LIST = 'list'
    UPDATE = 'update'
    ROTATE_CREDENTIALS = 'rotate-credentials'
    CATALOG = 'catalog'
    NAMESPACE = 'namespace'
    TABLE = 'table'
    VIEW = 'view'
    GRANT = 'grant'
    REVOKE = 'revoke'


class Actions:
    """
    Represents actions available to different subcommands available in the CLI. Currently, only some subcommands of the
    `privileges` command support actions.
    """

    GRANT = 'grant'
    REVOKE = 'revoke'


class Arguments:
    """
    Constants to represent different arguments used by various commands. This is a flattened view, and no one
    subcommand supports all these arguments. These argument names map directly to the parameters that the CLI expects
    and to the attribute names within the argparse Namespace generated by parsing.

    These values should be snake_case, but they will get mapped to kebab-case in `Parser.parse`
    """

    TYPE = 'type'
    REMOTE_URL = 'remote_url'
    DEFAULT_BASE_LOCATION = 'default_base_location'
    STORAGE_TYPE = 'storage_type'
    ALLOWED_LOCATION = 'allowed_location'
    ROLE_ARN = 'role_arn'
    EXTERNAL_ID = 'external_id'
    USER_ARN = 'user_arn'
    TENANT_ID = 'tenant_id'
    MULTI_TENANT_APP_NAME = 'multi_tenant_app_name'
    CONSENT_URL = 'consent_url'
    SERVICE_ACCOUNT = 'service_account'
    CATALOG_ROLE = 'catalog_role'
    CATALOG = 'catalog'
    PRINCIPAL = 'principal'
    CLIENT_ID = 'client_id'
    PRINCIPAL_ROLE = 'principal_role'
    PROPERTY = 'property'
    SET_PROPERTY = 'set_property'
    REMOVE_PROPERTY = 'remove_property'
    PRIVILEGE = 'privilege'
    NAMESPACE = 'namespace'
    TABLE = 'table'
    VIEW = 'view'
    CASCADE = 'cascade'
    CLIENT_SECRET = 'client_secret'
    ACCESS_TOKEN = 'access_token'
    HOST = 'host'
    PORT = 'port'
    BASE_URL = 'base_url'
    PARENT = 'parent'
    LOCATION = 'location'
    REGION = 'region'
    PROFILE = 'profile'


class Hints:
    """
    Constants used as hints by the various --help outputs. These are arranged within subclasses for readability, but
    there is no strict mapping between these subclasses and commands. For example, the hint for the `--catalog`
    parameter used by `catalog-roles create` and `catalog-roles delete` may be the same.
    """

    PROPERTY = ('A key/value pair such as: tag=value. Multiple can be provided by specifying this option'
                ' more than once')
    SET_PROPERTY = ('A key/value pair such as: tag=value. Merges the specified key/value into an existing'
                    ' properties map by updating the value if the key already exists or creating a new'
                    ' entry if not. Multiple can be provided by specifying this option more than once')
    REMOVE_PROPERTY = ('A key to remove from a properties map. If the key already does not exist then'
                       ' no action is takn for the specified key. If properties are also being set in'
                       ' the same update command then the list of removals is applied last. Multiple'
                       ' can be provided by specifying this option more than once')

    class Catalogs:
        GRANT = 'Grant a catalog role to a catalog'
        REVOKE = 'Revoke a catalog role from a catalog'

        class Create:
            TYPE = 'The type of catalog to create in [INTERNAL, EXTERNAL]. INTERNAL by default.'
            REMOTE_URL = '(For external catalogs) The remote URL to use'
            DEFAULT_BASE_LOCATION = '(Required) Default base location of the catalog'
            STORAGE_TYPE = '(Required) The type of storage to use for the catalog'
            ALLOWED_LOCATION = ('An allowed location for files tracked by the catalog. '
                                'Multiple locations can be provided by specifying this option more than once.')

            ROLE_ARN = '(Required for S3) A role ARN to use when connecting to S3'
            EXTERNAL_ID = '(Only for S3) The external ID to use when connecting to S3'
            REGION = '(Only for S3) The region to use when connecting to S3'
            USER_ARN = '(Only for S3) A user ARN to use when connecting to S3'

            TENANT_ID = '(Required for Azure) A tenant ID to use when connecting to Azure Storage'
            MULTI_TENANT_APP_NAME = '(Only for Azure) The app name to use when connecting to Azure Storage'
            CONSENT_URL = '(Only for Azure) A consent URL granting permissions for the Azure Storage location'

            SERVICE_ACCOUNT = '(Only for GCS) The service account to use when connecting to GCS'

        class Update:
            DEFAULT_BASE_LOCATION = 'A new default base location for the catalog'

    class Principals:
        class Create:
            TYPE = 'The type of principal to create in [SERVICE]'
            NAME = 'The principal name'
            CLIENT_ID = 'The output-only OAuth clientId associated with this principal if applicable'

        class Revoke:
            PRINCIPAL_ROLE = 'A principal role to revoke from this principal'

    class PrincipalRoles:
        PRINCIPAL_ROLE = 'The name of a principal role'
        LIST = 'List principal roles, optionally limited to those held a given principal'

        GRANT = 'Grant a principal role to a principal'
        REVOKE = 'Revoke a principal role from a principal'

        class Grant:
            PRINCIPAL = 'A principal to grant this principal role to'

        class Revoke:
            PRINCIPAL = 'A principal to revoke this principal role from'

        class List:
            CATALOG_ROLE = ('The name of a catalog role. If provided, show only principal roles assigned to this'
                            ' catalog role.')
            PRINCIPAL_NAME = ('The name of a principal. If provided, show only principal roles assigned to this'
                              ' principal.')

    class CatalogRoles:
        CATALOG_NAME = 'The name of an existing catalog'
        CATALOG_ROLE = 'The name of a catalog role'
        LIST = 'List catalog roles within a catalog. Optionally, specify a principal role.'
        REVOKE_CATALOG_ROLE = 'Revoke a catalog role from a principal role'
        GRANT_CATALOG_ROLE = 'Grant a catalog role to a principal role'

    class Grant:
        CATALOG_NAME = 'The name of a catalog'
        CATALOG_ROLE = 'The name of a catalog role'
        PRIVILEGE = 'The privilege to grant or revoke'
        NAMESPACE = 'A period-delimited namespace'
        TABLE = 'The name of a table'
        VIEW = 'The name of a view'
        CASCADE = 'When revoking privileges, additionally revoke privileges that depend on the specified privilege'

    class Namespaces:
        LOCATION = 'If specified, the location at which to store the namespace and entities inside it'
        PARENT = 'If specified, list namespaces inside this parent namespace'


UNIT_SEPARATOR = chr(0x1F)
CLIENT_ID_ENV = 'CLIENT_ID'
CLIENT_SECRET_ENV = 'CLIENT_SECRET'
CLIENT_PROFILE_ENV = 'CLIENT_PROFILE'
DEFAULT_HOSTNAME = 'localhost'
DEFAULT_PORT = 8181
CONFIG_DIR = os.environ.get('SCRIPT_DIR')
if CONFIG_DIR is None:
    raise Exception("The SCRIPT_DIR environment variable is not set. Please set it to the Polaris's script directory.")
CONFIG_FILE = os.path.join(CONFIG_DIR, '.polaris.json')
